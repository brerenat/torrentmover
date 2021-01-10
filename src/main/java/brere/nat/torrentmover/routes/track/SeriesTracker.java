package brere.nat.torrentmover.routes.track;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeriesTracker extends RouteBuilder {
	
	private static final Logger LOG = LoggerFactory.getLogger(SeriesTracker.class);

	@Override
	public void configure() throws Exception {
		LOG.info("Started Series Tracker Configure");
		
		from("quartz://tracker/series?cron={{tracker.quartz.cron}}").process(new SeriesTrackerProcessor())
		.end();
	}

}
