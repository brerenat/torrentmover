package com.home.torrentmover.routes.file;

import org.apache.camel.Processor;

public abstract class AbstractFileMoverProcessor implements Processor {

	protected static final String MOVIE = "Movie";
	protected static final String SERIES = "Series";

	protected final String movies;
	protected final String series;
	protected final String source;

	public AbstractFileMoverProcessor(final String movies, final String series, final String source) {
		this.movies = movies;
		this.series = series;
		this.source = source;
	}
}
