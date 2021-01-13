package brere.nat.torrentmover.routes.track;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.EntityManager;

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
import brere.nat.torrentmover.routes.file.FileUtils;
import brere.nat.transmission.rpc.api.RPCAPI;

public class TrackUtils {
	
	private static final Logger LOG = LoggerFactory.getLogger(TrackUtils.class);
	private static final int MAX_EP = 50;
	private static final int MAX_SN = 20;
	private static final String TEN_EIGHTY = "*1080";
	private static final String SEVEN_TWENTY = "*720";
	
	public static void pollSeries(final TorrentAPI torrentAPI, final RPCAPI rpcAPI, final OMDBAPI omdbAPI,
			final AutoPollSeries item, final EntityManager em)
			throws URISyntaxException, ClientProtocolException, IOException, TorrentAPIException {
		Map<Integer, Set<Integer>> baseMissingMap;
		Map<Integer, Set<Integer>> missingMap;
		Map<Integer, Set<Integer>> seasonMap;
		Set<Integer> eps;
		int maxSeasonNum;
		SeriesResult omdb;
		
		for (final AutoPollDownload download : item.getActiveDownloads()) {
			eps = item.getSeasonMap().getOrDefault(download.getSeason(), new HashSet<>());
			if (download.getEpisode() != 0) {
				eps.add(download.getEpisode());
			} else {
				for (int i = 1; i <= MAX_EP; i++) {
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
				searchIndividualEpisode(torrentAPI, rpcAPI, item, entry, em);
			} else {
				// Missing entire season search for whole season
				searchWholeSeason(torrentAPI, rpcAPI, item, entry, em);
			}
		}
	}

	private static void searchWholeSeason(final TorrentAPI torrentAPI, final RPCAPI rpcAPI, final AutoPollSeries item,
			final Entry<Integer, Set<Integer>> entry, final EntityManager em)
			throws TorrentAPIException, URISyntaxException, ClientProtocolException, IOException {
		LOG.info("Searching whole season :" + entry.getKey());
		try {
			getTorrent(torrentAPI, rpcAPI, item, getFullSeasonString(entry.getKey(), SEVEN_TWENTY), entry.getKey(), 0, em);
		} catch (TorrentNotFoundException e) {
			LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getFullSeasonString(entry.getKey(), SEVEN_TWENTY));
			
			try {
				getTorrent(torrentAPI, rpcAPI, item, getFullSeasonString(entry.getKey(), TEN_EIGHTY), entry.getKey(), 0, em);
			} catch (TorrentNotFoundException e2) {
				LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getFullSeasonString(entry.getKey(), TEN_EIGHTY));
				
				try {
					getTorrent(torrentAPI, rpcAPI, item, getFullSeasonString(entry.getKey(), null), entry.getKey(), 0, em);
				} catch (TorrentNotFoundException e3) {
					LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getFullSeasonString(entry.getKey(), null));
					LOG.warn("Couldn't find whole season, searching individually");
					searchIndividualEpisode(torrentAPI, rpcAPI, item, entry, em);
				}
			}
		}
	}

	private static void searchIndividualEpisode(final TorrentAPI torrentAPI, final RPCAPI rpcAPI, final AutoPollSeries item,
			final Entry<Integer, Set<Integer>> entry, final EntityManager em)
			throws TorrentAPIException, URISyntaxException, ClientProtocolException, IOException {
		int counter = 0;
		for (final Integer episode : entry.getValue()) {
			if (counter < 3) {
				LOG.info("Searching episode :" + getSeasonEpisodeString(entry.getKey(), episode, null));
				try {
					getTorrent(torrentAPI, rpcAPI, item, getSeasonEpisodeString(entry.getKey(), episode, SEVEN_TWENTY), entry.getKey(), episode, em);
				} catch (TorrentNotFoundException e) {
					LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getSeasonEpisodeString(entry.getKey(), episode, SEVEN_TWENTY));
					
					try {
						getTorrent(torrentAPI, rpcAPI, item, getSeasonEpisodeString(entry.getKey(), episode, TEN_EIGHTY), entry.getKey(), episode, em);
					} catch (TorrentNotFoundException e2) {
						LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getSeasonEpisodeString(entry.getKey(), episode, TEN_EIGHTY));
						try {
							getTorrent(torrentAPI, rpcAPI, item, getSeasonEpisodeString(entry.getKey(), episode, null), entry.getKey(), episode, em);
						} catch (TorrentNotFoundException e3) {
							LOG.warn("Couldn't find a torrent for " + item.getTitle() + " " + getSeasonEpisodeString(entry.getKey(), episode, null));
							counter++;
						}
					}
				}
			}
		}
	}

	private static void getTorrent(final TorrentAPI torrentAPI, final RPCAPI rpcAPI, final AutoPollSeries item,
			final String search, final int seasonNum, final int epNum, final EntityManager em)
			throws TorrentAPIException, URISyntaxException, ClientProtocolException, IOException {
		final List<TorrentResult> torrents = torrentAPI.getTorrentByIMDBIDAndSearch(item.getImdbID(), search);
		
		if (torrents != null && !torrents.isEmpty()) {
			boolean match;
			AutoPollDownload downloaded;
			boolean started = false;
			for (TorrentResult torrent : torrents) {
				LOG.info("Title :" + torrent.getTitle());
				match = FileUtils.EPISODEPATTERN.matcher(torrent.getTitle()).find();
				LOG.info("Ep Number :" + epNum);
				if ((epNum == 0 && !match) || (epNum != 0 && match)) {
					LOG.info("Found torrent, uploading to rpc");
					rpcAPI.addTorrent(torrent.getDownload());
					
					downloaded = new AutoPollDownload(item);
					downloaded.setSeason(seasonNum);
					downloaded.setEpisode(epNum);
					em.persist(downloaded);
					LOG.info("Saving AutoPollDownload");
					started = true;
				}
			}
			
			if (!started) {
				throw new TorrentNotFoundException("No Torrents found");
			}
			
		} else {
			throw new TorrentNotFoundException("No Torrents found");
		}
	}
	
	private static String getSeasonEpisodeString(final int season, final int episode, final String quality) {
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
	
	private static String getFullSeasonString(final int season, final String quality) {
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
	private static Map<Integer, Set<Integer>> getBaseMissingMap(final int maxSeason, final int maxEpisode) {
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

}
