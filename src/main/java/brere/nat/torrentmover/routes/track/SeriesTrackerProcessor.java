package brere.nat.torrentmover.routes.track;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import brere.nat.mydb.model.AutoPollSeries;

public class SeriesTrackerProcessor implements Processor {
	
	private static final int MAX_EP = 50;

	@Override
	public void process(Exchange exchange) throws Exception {
		// TODO Get list of Auto Polls
		
		final List<AutoPollSeries> series = AutoPollSeries.Queries.getAllByActive(true);
		
		// TODO Check file system for which episodes are missing
		// TODO Check AutoPollDownload for which are currently being downloaded
		
		for (final AutoPollSeries item : series) {
			
		}
		
		
		// TODO Work out what episodes of which seasons we need to download and call torrent-api for those torrents
		
		// TODO Using the magnet links from those torrents add them using the transmission rpc api
		
		// TODO Insert which ones were added successful into the AutoPollDownload table
	}

}
