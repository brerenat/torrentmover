package brere.nat.torrentmover.routes.track.quartz;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brere.nat.mydb.model.AutoPollDownload;
import brere.nat.mydb.model.AutoPollSeries;
import brere.nat.omdbapi.api.OMDBAPI;
import brere.nat.torrent.api.TorrentAPI;
import brere.nat.torrentmover.SpringStart;
import brere.nat.torrentmover.routes.track.TrackUtils;
import brere.nat.transmission.rpc.api.RPCAPI;

public class SeriesTrackerProcessor implements Processor {
	
	private static final Logger LOG = LoggerFactory.getLogger(SeriesTrackerProcessor.class);

	@Override
	public void process(Exchange exchange) throws Exception {
		LOG.info("Starting to auto Poll Series");
		final String torrentName = SpringStart.getProp().getProperty("tracker.torrent.name");
		final TorrentAPI torrentAPI = new TorrentAPI(torrentName);
		torrentAPI.setMinSeeders(3);
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

		final TypedQuery<AutoPollSeries> getAllActive = em.createNamedQuery("AutoPollSeries.getAllByActive", AutoPollSeries.class);
		getAllActive.setParameter("active", true);
		
		List<AutoPollSeries> series = getAllActive.getResultList();
		
		List<AutoPollDownload> toDelete = new ArrayList<>();
		Set<Integer> eps;
		LOG.info("Number of Series to poll :" + series.size());
		for (final AutoPollSeries item : series) {
			LOG.info("Polling :" + item.getTitle());
			item.setSeasonMap(TrackUtils.updateSeasonMapFromFileSystem(item.getFolderName()));
			
			for (final AutoPollDownload download : item.getActiveDownloads()) {
				eps = item.getSeasonMap().getOrDefault(download.getSeason(), new HashSet<>());
				if (eps.contains(download.getEpisode())) {
					toDelete.add(download);
				}
			}
		}
		
		em.flush();
		
		series = getAllActive.getResultList();
		
		for (final AutoPollSeries item : series) {
			TrackUtils.pollSeries(torrentAPI, rpcAPI, omdbAPI, item, em);
		}
		
		transaction.commit();
		em.close();
	}
	
}
