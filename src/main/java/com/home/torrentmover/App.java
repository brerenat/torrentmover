package com.home.torrentmover;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.home.torrentmover.routes.file.FileMover;

public class App {
	private final Main main;
	private static final Logger LOG = LoggerFactory.getLogger(App.class);
	
	public App() {
		super();
		this.main = new Main();
	}
	
	public static void main(String[] args) {
		final String source;
		final String movies;
		final String series;
		
		
		if (args.length > 0) {
			source = args[0];
		} else {
			source = "/drives/storage/Videos/Downloads/";
		}
		if (args.length > 1) {
			movies = args[1];
		} else {
			movies = "/drives/storage/Videos/Movies/";
		}
		if (args.length > 2) {
			series = args[2];
		} else {
			series = "/drives/storage/Videos/Series/";
		}
		
		new App().startCamel(source, movies, series);
	}
	
	private void startCamel(final String source, final String movies, final String series) {
		final CamelContext context = new DefaultCamelContext();
		try {
			LOG.info("Adding Routes");
			context.addRoutes(new FileMover(new StringBuilder("file:").append(source).append("?recursive=true&noop=true&include=.*.mkv|.*.mp4|.*.avi").toString(), movies, series));
			
            main.setDuration(-1);
            main.getCamelContexts().add(context);
            main.start();
            while(main.isStarting()) {
            	Thread.sleep(100);
            }
            LOG.info("Camel Started :" + main.isStarted());
            Thread.sleep(300000L);
		} catch (Exception e) {
			LOG.error(e.getClass().getName() + " when Running FileMover Route", e);
		}
	}
}
