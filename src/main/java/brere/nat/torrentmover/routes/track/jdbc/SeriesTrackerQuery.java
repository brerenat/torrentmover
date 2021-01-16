package brere.nat.torrentmover.routes.track.jdbc;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;

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
		final EntityManager em = SpringStart.getEm();
		EntityTransaction transaction = em.getTransaction();
		transaction.begin();
		try {
			final TypedQuery<AutoPollSeries> getAllActive = em.createNamedQuery("AutoPollSeries_getAllByStartPoll", AutoPollSeries.class);
			getAllActive.setParameter("startPoll", true);
			
			final AutoPollSeries series = getAllActive.getSingleResult();
			series.setStartPoll(false);
			exchange.getOut().setBody(series, AutoPollSeries.class);
		} catch (NoResultException e) {
			LOG.debug("Nothing to Start Poll");
		} finally {
			transaction.commit();
			em.close();
		}
		
	}
	

}
