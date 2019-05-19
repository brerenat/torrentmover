package com.home.torrentmover;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.home.torrentmover.routes.file.FileMover;

public class CamelStarter implements Runnable {
	
	private static final Logger LOG = LoggerFactory.getLogger(CamelStarter.class);
	private final String source;
	private final String movies;
	private final String series;

	public CamelStarter(final String source, final String movies, final String series) {
		super();
		this.source = source;
		this.movies = movies;
		this.series = series;
	}
	
	@Override
	public void run() {
		final CamelContext context = new DefaultCamelContext();
		try {
			LOG.info("Adding Routes");
			context.addRoutes(new FileMover(new StringBuilder("file:").append(source).append("?recursive=true&noop=true&include=.*.mkv|.*.mp4|.*.avi").toString(), movies, series));
			
            context.start();
            Thread.sleep(1000000);
		} catch (Exception e) {
			LOG.error(e.getClass().getName() + " when Running FileMover Route", e);
		}
		
	}
	

}
