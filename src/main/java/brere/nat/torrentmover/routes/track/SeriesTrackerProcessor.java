package brere.nat.torrentmover.routes.track;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brere.nat.mydb.model.AutoPollDownload;
import brere.nat.mydb.model.AutoPollSeries;
import brere.nat.omdbapi.api.OMDBAPI;
import brere.nat.omdbapi.model.SeriesResult;
import brere.nat.torrent.api.TorrentAPI;
import brere.nat.torrent.api.model.TorrentResult;
import brere.nat.torrent.api.utils.TorrentAPIException;
import brere.nat.torrent.api.utils.TorrentNotFoundException;
import brere.nat.torrentmover.SpringStart;
import brere.nat.torrentmover.routes.file.FileUtils;
import brere.nat.transmission.rpc.api.RPCAPI;

public class SeriesTrackerProcessor implements Processor {
	
	private static final Logger LOG = LoggerFactory.getLogger(SeriesTrackerProcessor.class);
	private static final String EE = "[Ee]";
	private static final int MAX_EP = 50;
	private static final int MAX_SN = 20;
	private static final String TEN_EIGHTY = "*1080";
	private static final String SEVEN_TWENTY = "*720";

	@Override
	public void process(Exchange exchange) throws Exception {
		LOG.info("Starting to auto Poll Series");
		final String torrentName = SpringStart.getProp().getProperty("tracker.torrent.name");
		final TorrentAPI torrentAPI = new TorrentAPI(torrentName);
		final String rpcHost = SpringStart.getProp().getProperty("tracker.rpc.hostname");
		final RPCAPI rpcAPI;
		if (rpcHost != null) {
			rpcAPI = new RPCAPI(rpcHost);
		} else {
			rpcAPI = new RPCAPI();
		}
		
		final String omdbKey = SpringStart.getProp().getProperty("tracker.omdb.key");
		final OMDBAPI omdbAPI = (omdbKey != null) ? new OMDBAPI(omdbKey) : null;
		
		final List<AutoPollSeries> series = AutoPollSeries.Queries.getAllByActive(true);
		
		Map<Integer, Set<Integer>> baseMissingMap;
		Map<Integer, Set<Integer>> missingMap;
		Map<Integer, Set<Integer>> seasonMap;
		Set<Integer> eps;
		int maxSeasonNum;
		SeriesResult omdb;
		LOG.info("Number of Series to poll :" + series.size());
		for (final AutoPollSeries item : series) {
			LOG.info("Polling :" + item.getTitle());
			item.setSeasonMap(updateSeasonMapFromFileSystem(item.getFolderName()));
			
			for (final AutoPollDownload download : item.getActiveDownloads()) {
				eps = item.getSeasonMap().getOrDefault(download.getSeason(), new HashSet<>());
				if (download.getEpisode() != 0) {
					eps.add(download.getEpisode());
				} else {
					for (int i = 0; i <= MAX_EP; i++) {
						eps.add(i);
					}
				}
				
				item.getSeasonMap().put(download.getSeason(), eps);
			}
			
			maxSeasonNum = MAX_SN;
			if (omdbAPI != null) {
				omdb = omdbAPI.getSeriesByIMDBID(item.getImdbID());
				if (omdb != null) {
					maxSeasonNum = omdb.getTotalSeasons();
				}
			}
			
			LOG.info("Max Seasons :" + maxSeasonNum);
			
			seasonMap = item.getSeasonMap();
			baseMissingMap = getBaseMissingMap(maxSeasonNum, MAX_EP);
			missingMap = new HashMap<>();
			for (final Entry<Integer, Set<Integer>> entry : baseMissingMap.entrySet()) {
				if (!seasonMap.containsKey(entry.getKey())) {
					// Don't have this season
					LOG.info("Don't have season " + entry.getKey());
					missingMap.put(entry.getKey(), entry.getValue());
				} else {
					// Have this season
					LOG.info(seasonMap.get(entry.getKey()).size() + " Eps for Season :" + entry.getKey());
					for (final Integer episode : entry.getValue()) {
						// Don't have this episode
						if (!seasonMap.get(entry.getKey()).contains(episode)) {
							// Add this season if not already should always have it at this point
							eps = missingMap.getOrDefault(entry.getKey(), new HashSet<>());
							eps.add(episode);
							missingMap.put(entry.getKey(), eps);
						}
					}
				}
			}
			
			LOG.info(item.getTitle() + " Missing Map :" + missingMap);
			
			// missingMap should have all the seasons and episode numbers to search for
			for (final Entry<Integer, Set<Integer>> entry : missingMap.entrySet()) {
				LOG.info("Number of Episodes missing for Season " + entry.getKey() + " :" + entry.getValue().size());
				if (entry.getValue().size() != MAX_EP) {
					searchIndividualEpisode(torrentAPI, rpcAPI, item, entry);
				} else {
					// Missing entire season search for whole season
					searchWholeSeason(torrentAPI, rpcAPI, item, entry);
				}
			}
		}
	}

	private void searchWholeSeason(final TorrentAPI torrentAPI, final RPCAPI rpcAPI, final AutoPollSeries item,
			final Entry<Integer, Set<Integer>> entry)
			throws TorrentAPIException, URISyntaxException, ClientProtocolException, IOException {
		LOG.info("Searching whole season :" + entry.getKey());
		try {
			getTorrent(torrentAPI, rpcAPI, item, getFullSeasonString(entry.getKey(), SEVEN_TWENTY), entry.getKey(), 0);
		} catch (TorrentNotFoundException e) {
			LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getFullSeasonString(entry.getKey(), SEVEN_TWENTY));
			
			try {
				getTorrent(torrentAPI, rpcAPI, item, getFullSeasonString(entry.getKey(), TEN_EIGHTY), entry.getKey(), 0);
			} catch (TorrentNotFoundException e2) {
				LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getFullSeasonString(entry.getKey(), TEN_EIGHTY));
				
				try {
					getTorrent(torrentAPI, rpcAPI, item, getFullSeasonString(entry.getKey(), null), entry.getKey(), 0);
				} catch (TorrentNotFoundException e3) {
					LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getFullSeasonString(entry.getKey(), null));
					LOG.warn("Couldn't find whole season, searching individually");
					searchIndividualEpisode(torrentAPI, rpcAPI, item, entry);
				}
			}
		}
	}

	private void searchIndividualEpisode(final TorrentAPI torrentAPI, final RPCAPI rpcAPI, final AutoPollSeries item,
			final Entry<Integer, Set<Integer>> entry)
			throws TorrentAPIException, URISyntaxException, ClientProtocolException, IOException {
		for (final Integer episode : entry.getValue()) {
			LOG.info("Searching episode :" + getSeasonEpisodeString(entry.getKey(), episode, null));
			try {
				getTorrent(torrentAPI, rpcAPI, item, getSeasonEpisodeString(entry.getKey(), episode, SEVEN_TWENTY), entry.getKey(), episode);
			} catch (TorrentNotFoundException e) {
				LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getSeasonEpisodeString(entry.getKey(), episode, SEVEN_TWENTY));
				
				try {
					getTorrent(torrentAPI, rpcAPI, item, getSeasonEpisodeString(entry.getKey(), episode, TEN_EIGHTY), entry.getKey(), episode);
				} catch (TorrentNotFoundException e2) {
					LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getSeasonEpisodeString(entry.getKey(), episode, TEN_EIGHTY));
					try {
						getTorrent(torrentAPI, rpcAPI, item, getSeasonEpisodeString(entry.getKey(), episode, null), entry.getKey(), episode);
					} catch (TorrentNotFoundException e3) {
						LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getSeasonEpisodeString(entry.getKey(), episode, null));
					}
				}
			}
		}
	}

	private void getTorrent(final TorrentAPI torrentAPI, final RPCAPI rpcAPI, final AutoPollSeries item,
			final String search, final int seasonNum, final int epNum)
			throws TorrentAPIException, URISyntaxException, ClientProtocolException, IOException {
		final List<TorrentResult> torrents = torrentAPI.getTorrentByIMDBIDAndSearch(item.getImdbID(), search);
		if (torrents != null && !torrents.isEmpty()) {
			LOG.info("Found torrent, uploading to rpc");
			rpcAPI.addTorrent(torrents.get(0).getDownload());
			
			final AutoPollDownload downloaded = new AutoPollDownload(item);
			downloaded.setSeason(seasonNum);
			downloaded.setEpisode(epNum);
			item.getActiveDownloads().add(downloaded);
			SpringStart.getEm().persist(downloaded);
		} else {
			throw new TorrentNotFoundException("No Torrents found");
		}
	}
	
	private String getSeasonEpisodeString(final int season, final int episode, final String quality) {
		final StringBuilder str = new StringBuilder("S");
		if (season < 10) {
			str.append("0");
		}
		str.append(season).append("E");
		if (episode < 10) {
			str.append("0");
		}
		str.append(episode);
		if (quality != null) {
			str.append(quality);
		}
		return str.toString();
	}
	
	private String getFullSeasonString(final int season, final String quality) {
		final StringBuilder str = new StringBuilder("S");
		if (season < 10) {
			str.append("0");
		}
		str.append(season);
		if (quality != null) {
			str.append(quality);
		}
		return str.toString();
	}
	
	/**
	 * 
	 * @param maxSeason
	 * @param maxEpisode
	 * @return
	 */
	private Map<Integer, Set<Integer>> getBaseMissingMap(final int maxSeason, final int maxEpisode) {
		final Map<Integer, Set<Integer>> map = new HashMap<>();
		Set<Integer> episodes;
		for (int i = 1; i <= maxSeason; i++) {
			episodes = new HashSet<>();
			for (int j = 1; j <= maxEpisode; j++) {
				episodes.add(j);
			}
			map.put(i, episodes);
		}
		
		return map;
	}

	/**
	 * 
	 * @param item
	 * @throws IOException
	 */
	public Map<Integer, Set<Integer>> updateSeasonMapFromFileSystem(final String folder) throws Exception {
		final Map<Integer, Set<Integer>> seasonMap = new HashMap<>();
		int seasonNum;
		int epNum;
		Matcher matches;
		Set<Integer> eps;
		String seasonName;
		String episodeName;
		String fileName;
		final Path seriesFolder = Paths.get(folder);
		if (Files.exists(seriesFolder)) {
			Files.createDirectories(seriesFolder);
			setPermissions(seriesFolder);
		}
		
		LOG.info("Series dir :" + seriesFolder);
		try (final DirectoryStream<Path> dirs = Files.newDirectoryStream(seriesFolder)) {
			for (final Path seasonDir : dirs) {
				seasonName = seasonDir.getFileName().toString();
				LOG.info("Folder :" + seasonName);
				if (seasonName.startsWith("Season ")) {
					LOG.info("Is Season");
					seasonNum = Integer.valueOf(seasonName.toString().replace("Season ", ""));
					LOG.info("Season Number :" + seasonNum);
					try (final DirectoryStream<Path> files = Files.newDirectoryStream(seasonDir)) {
						for (final Path file : files) {
							fileName = file.getFileName().toString();
							LOG.info("File under " + seasonName + " :" + fileName);
							matches = FileUtils.EP_ONLY_PATTERN.matcher(fileName);
							LOG.info("Trying Match");
							if (matches.find()) {
								episodeName = matches.group();
								LOG.info("Found Match :" + episodeName);
								epNum = Integer.valueOf(episodeName.replaceAll(EE, ""));
								eps = seasonMap.getOrDefault(seasonNum, new HashSet<>());
								eps.add(epNum);
								seasonMap.put(seasonNum, eps);
							}
						}
					}
				}
			}
		}
		LOG.info("Out Season Map :" + seasonMap);
		return seasonMap;
	}

	/**
	 * 
	 * @param seriesFolder
	 * @throws IOException
	 */
	private void setPermissions(Path seriesFolder) throws IOException {
		if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
			final Set<PosixFilePermission> perms = Files.readAttributes(seriesFolder, PosixFileAttributes.class).permissions();
			
		    perms.add(PosixFilePermission.OWNER_WRITE);
		    perms.add(PosixFilePermission.OWNER_READ);
		    perms.add(PosixFilePermission.OWNER_EXECUTE);
		    perms.add(PosixFilePermission.GROUP_WRITE);
		    perms.add(PosixFilePermission.GROUP_READ);
		    perms.add(PosixFilePermission.GROUP_EXECUTE);
		    perms.add(PosixFilePermission.OTHERS_WRITE);
		    perms.add(PosixFilePermission.OTHERS_READ);
		    perms.add(PosixFilePermission.OTHERS_EXECUTE);
		    Files.setPosixFilePermissions(seriesFolder, perms);
		}
	}

}
