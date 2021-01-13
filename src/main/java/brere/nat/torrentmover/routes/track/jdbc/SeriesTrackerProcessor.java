package brere.nat.torrentmover.routes.track.jdbc;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
		final AutoPollSeries item = exchange.getIn().getBody(AutoPollSeries.class);
		if (item != null) {
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
			TrackUtils.pollSeries(torrentAPI, rpcAPI, omdbAPI, item, em);
			transaction.commit();
		}
	}

}
