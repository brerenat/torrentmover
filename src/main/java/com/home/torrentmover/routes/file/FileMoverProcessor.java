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

	private static final String EPISODEREGEX = "S\\d{1,}E\\d{1,}";

	private static final Logger LOG = LoggerFactory.getLogger(FileMoverProcessor.class);

	private static final Pattern EPISODEPATTERN = Pattern.compile(EPISODEREGEX, Pattern.CASE_INSENSITIVE);
	private final String movies;
	private final String series;
	private final String source;

	public FileMoverProcessor(final String movies, final String series, final String source) {
		super();
		this.movies = movies;
		this.series = series;
		this.source = source;
	}

	@Override
	public void process(final Exchange exchange) throws Exception {
		LOG.info("Starting to move File");
		final GenericFile<?> body = exchange.getIn().getBody(GenericFile.class);

		LOG.info("Body :" + body.getFileNameOnly());
		File source = new File(body.getAbsoluteFilePath());
		while (!source.canWrite()) {
			Thread.sleep(1000L);
			source = new File(body.getAbsoluteFilePath());
		}
		
		final Matcher nameMatcher = EPISODEPATTERN.matcher(body.getFileNameOnly());
		final String ext = body.getFileNameOnly().substring(body.getFileNameOnly().lastIndexOf('.'));
		final String fileName = body.getFileNameOnly().substring(0, body.getFileNameOnly().lastIndexOf('.'));
		File destination = null;
		if (nameMatcher.find()) {
			String seriesName = fileName.split(EPISODEREGEX)[0].replace('.', ' ').replace('_', ' ').replaceAll("\\[(.*?)\\]", "").replaceAll("\\((.*?)\\)", "").trim();
			final File seriesDir = new File(series);
			LOG.info("Series Name :" + seriesName);
			File seriesFolderFound = null;
			for (final File dir : seriesDir.listFiles()) {
				if (dir.isDirectory() && (dir.getName().equalsIgnoreCase(seriesName) || dir.getName().contains(seriesName))) {
					seriesFolderFound = dir;
					seriesName = dir.getName();
				}
			}
			if (seriesFolderFound == null) {
				final File newDir = new File(series + seriesName);
				newDir.mkdir();
				destination = new File(newDir.getAbsolutePath() + "/" + fileName + ext);
				source.renameTo(destination);
			} else {
				LOG.info("Found Series Folder");
				destination = new File(seriesFolderFound.getAbsolutePath() + "/" + seriesName + " " + nameMatcher.group() + ext);
				source.renameTo(destination);
			}
		} else {
			destination = new File(movies + fileName.replace('.', ' ').replace('_', ' ').replaceAll("\\[(.*?)\\]", "").replaceAll("\\((.*?)\\)", "") + ext);
			source.renameTo(destination);
		}

		final File parentDir = source.getParentFile();
		LOG.info("Parent Dir :" + parentDir.getAbsoluteFile());
		final File[] files = parentDir.listFiles(new ExtensionFilter());
		LOG.info("Number of Files :" + files.length);
		if (files.length == 0) {
			File[] fileArr = new File[parentDir.listFiles().length + 1];
			fileArr[parentDir.listFiles().length] = parentDir;
			emptyParent(fileArr);
		}

		LOG.info("New File Name :" + destination.getAbsoluteFile());
	}

	private void emptyParent(final File[] files) {
		for (final File file : files) {
			LOG.info("File To Check :" + file.getAbsoluteFile());
			if (file.isDirectory() && file.listFiles().length > 0) {
				LOG.info("Is not empty Dir");
				emptyParent(file.listFiles());
			}
			if (!file.getAbsolutePath().equals(source) && !(file.getAbsolutePath() + "/").equals(source)) {
				LOG.info("Deleting :" + file.getAbsoluteFile());
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
