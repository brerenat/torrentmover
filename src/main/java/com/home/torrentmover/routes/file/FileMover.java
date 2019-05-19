package com.home.torrentmover.routes.file;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileMover extends RouteBuilder {

	private final String from;
	private static final Logger LOG = LoggerFactory.getLogger(FileMover.class);
	private final String movies;
	private final String series;
	
	public FileMover(final String from, final String movies, final String series) {
		this.from = from;
		this.movies = movies;
		this.series = series;
	}
	
	@Override
	public void configure() throws Exception {
		LOG.info("Starting Configure");
		from(from).process(new FileMoverProcessor(movies, series)).end();
	}

}
