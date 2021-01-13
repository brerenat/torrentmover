package brere.nat.torrentmover.routes.track.jdbc;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SeriesTrackerJDBC extends RouteBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(SeriesTrackerJDBC.class);

	@Override
	public void configure() throws Exception {
		LOG.info("Configuring " + this.getClass().getName());
		
		from("timer://PollStart?period=60000")
		.process(new SeriesTrackerQuery())
		.process(new SeriesTrackerProcessor())
		.end();

	}

}
