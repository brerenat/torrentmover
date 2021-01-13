package brere.nat.torrentmover.routes.track.quartz;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SeriesTrackerQuartz extends RouteBuilder {
	
	private static final Logger LOG = LoggerFactory.getLogger(SeriesTrackerQuartz.class);

	@Override
	public void configure() throws Exception {
		LOG.info("Configuring " + this.getClass().getName());
		
		from("quartz2://tracker/series?cron={{tracker.quartz.cron}}").process(new SeriesTrackerProcessor())
		.end();
	}

}
