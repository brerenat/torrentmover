package brere.nat.torrentmover.routes.track.jdbc;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brere.nat.mydb.model.AutoPollSeries;
import brere.nat.torrentmover.SpringStart;

public class SeriesTrackerQuery implements Processor {
	
	private static final Logger LOG = LoggerFactory.getLogger(SeriesTrackerQuery.class);

	@Override
	public void process(final Exchange exchange) throws Exception {
		LOG.info("Starting to auto Poll Series");
		try {
			final EntityManager em = SpringStart.getEm();
			EntityTransaction transaction = em.getTransaction();
			transaction.begin();
			final AutoPollSeries series = AutoPollSeries.Queries.getAllByStartPoll(true, em);
			series.setStartPoll(false);
			transaction.commit();
			exchange.getOut().setBody(series, AutoPollSeries.class);
		} catch (NoResultException e) {
			LOG.debug("Nothing to Start Poll");
		}
		
	}
	

}
