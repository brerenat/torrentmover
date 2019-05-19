package com.home.torrentmover.routes.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileMoverProcessor implements Processor {

	private static final Logger LOG = LoggerFactory.getLogger(FileMoverProcessor.class);

	private static final Pattern EPISODEPATTERN = Pattern.compile("S\\d{1,}E\\d{1,}", Pattern.CASE_INSENSITIVE);
	private final String movies;
	private final String series;

	public FileMoverProcessor(final String movies, final String series) {
		super();
		this.movies = movies;
		this.series = series;
	}

	@Override
	public void process(final Exchange exchange) throws Exception {
		LOG.info("Starting to move File");
		final GenericFile<?> body = exchange.getIn().getBody(GenericFile.class);

		LOG.info("Body :" + body.getFileNameOnly());
		final File source = new File(body.getAbsoluteFilePath());
		final Matcher nameMatcher = EPISODEPATTERN.matcher(body.getFileNameOnly());
		if (nameMatcher.find()) {
			
			source.renameTo(new File(series + body.getFileNameOnly()));
		} else {
			source.renameTo(new File(movies + body.getFileNameOnly()));
		}

		final File parentDir = source.getParentFile();
		final File[] files = parentDir.listFiles(new ExtensionFilter());
		if (files.length == 0) {
			emptyParent(parentDir.listFiles());
		}

		LOG.info("New File Name :" + source.getPath());
	}

	private void emptyParent(final File[] files) {
		for (final File file : files) {
			if (file.isDirectory()) {
				emptyParent(file.listFiles());
			} else {
				file.delete();
			}
		}
	}

	private class ExtensionFilter implements FilenameFilter {
		public boolean accept(File dir, String name) {
			String lowercaseName = name.toLowerCase();
			if (lowercaseName.endsWith(".mkv") || lowercaseName.endsWith(".mp4") || lowercaseName.endsWith(".avi")) {
				return true;
			} else {
				return false;
			}
		}
	}

}
