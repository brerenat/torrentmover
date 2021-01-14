package brere.nat.torrentmover.routes.track.quartz;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brere.nat.mydb.model.AutoPollDownload;
import brere.nat.mydb.model.AutoPollSeries;
import brere.nat.mydb.utils.ProcessUtils;
import brere.nat.omdbapi.api.OMDBAPI;
import brere.nat.torrent.api.TorrentAPI;
import brere.nat.torrentmover.SpringStart;
import brere.nat.torrentmover.routes.file.FileUtils;
import brere.nat.torrentmover.routes.track.TrackUtils;
import brere.nat.transmission.rpc.api.RPCAPI;

public class SeriesTrackerProcessor implements Processor {
	
	private static final Logger LOG = LoggerFactory.getLogger(SeriesTrackerProcessor.class);
	private static final String EE = "[Ee]";

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
		
		final EntityManager em = SpringStart.getEm();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();

		final TypedQuery<AutoPollSeries> getAllActive = em.createNamedQuery("AutoPollSeries_getAllByActive", AutoPollSeries.class);
		getAllActive.setParameter("active", true);
		
		List<AutoPollSeries> series = getAllActive.getResultList();
		
		List<AutoPollDownload> toDelete = new ArrayList<>();
		Set<Integer> eps;
		LOG.info("Number of Series to poll :" + series.size());
		for (final AutoPollSeries item : series) {
			LOG.info("Polling :" + item.getTitle());
			item.setSeasonMap(updateSeasonMapFromFileSystem(item.getFolderName()));
			
			for (final AutoPollDownload download : item.getActiveDownloads()) {
				eps = item.getSeasonMap().getOrDefault(download.getSeason(), new HashSet<>());
				if (eps.contains(download.getEpisode())) {
					toDelete.add(download);
				}
			}
			
			for (final AutoPollDownload download : toDelete) {
				ProcessUtils.getEm().remove(download);
			}
		}
		
		em.flush();
		
		series = getAllActive.getResultList();
		
		for (final AutoPollSeries item : series) {
			TrackUtils.pollSeries(torrentAPI, rpcAPI, omdbAPI, item, em);
		}
		
		transaction.commit();
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
		if (!Files.exists(seriesFolder)) {
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
