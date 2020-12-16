package com.home.torrentmover.routes.file;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.home.torrentmover.SpringStart;

public class FileMoverProcessor extends AbstractFileMoverProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(FileMoverProcessor.class);

	public FileMoverProcessor() {
		super(SpringStart.getProp().getProperty("movies"), SpringStart.getProp().getProperty("series"),
				SpringStart.getProp().getProperty("source"));
	}

	public void process(final Exchange exchange) throws Exception {
		LOG.info("Starting to move File");
		final GenericFile<?> body = exchange.getIn().getBody(GenericFile.class);

		LOG.info("Body :" + body.getFileNameOnly());
		final File source = FileUtils.checkFile(body.getAbsoluteFilePath());

		final Matcher nameMatcher = FileUtils.EPISODEPATTERN.matcher(body.getFileNameOnly());
		final String ext = body.getFileNameOnly().substring(body.getFileNameOnly().lastIndexOf('.'));
		final String fileName = body.getFileNameOnly().substring(0, body.getFileNameOnly().lastIndexOf('.'));
		final File destination;
		final String fileTypeStr;
		final boolean uppercase = Boolean.getBoolean(SpringStart.getProp().getProperty("file.uppercase.firstchar"));
		if (nameMatcher.find()) {
			fileTypeStr = SERIES;
			String seriesName = FileUtils.getSeriesName(fileName, uppercase);
			final File seriesDir = new File(series);
			LOG.info("Series Name :" + seriesName);
			File seriesFolderFound = FileUtils.getExistingFolder(seriesName, seriesDir);
			if (seriesFolderFound == null) {
				final File newDir = new File(series + seriesName);
				newDir.mkdir();
				destination = new File(new StringBuilder(newDir.getAbsolutePath()).append(File.separatorChar).append(seriesName)
						.append(" ").append(nameMatcher.group().toUpperCase()).append(ext).toString());
			} else {
				LOG.info("Found Series Folder");
				destination = new File(new StringBuilder(seriesFolderFound.getAbsolutePath()).append(File.separatorChar).append(seriesFolderFound.getName())
						.append(" ").append(nameMatcher.group().toUpperCase()).append(ext).toString());
			}
		} else {
			fileTypeStr = MOVIE;
			LOG.info("File Name :" + fileName);
			String newFileName = FileUtils.getMovieName(fileName, uppercase);
			LOG.info("New File Name :" + newFileName);
			final Matcher matches = FileUtils.YEARPATTERN.matcher(newFileName);
			if (matches.find()) {
				LOG.info("Found Year String");
				newFileName = new StringBuilder(newFileName.split(FileUtils.YEARREGEX)[0]).append("(").append(matches.group()).append(")").toString().trim();
				LOG.info("New File Name :" + newFileName);
			}
			
			final File moviesDir = new File(movies);
			File moviesFolderFound = FileUtils.getExistingFolder(newFileName, moviesDir);
			
			final String movieNameNoExt;
			
			if (moviesFolderFound == null) {
				final File newDir = new File(movies + newFileName);
				newDir.mkdir();
				movieNameNoExt = new StringBuilder(newDir.getAbsolutePath()).append(File.separatorChar).append(newFileName).toString();
				destination = new File(new StringBuilder(movieNameNoExt).append(ext).toString());
			} else {
				LOG.info("Found Movies Folder");
				movieNameNoExt = new StringBuilder(moviesFolderFound.getAbsolutePath()).append(File.separatorChar).append(newFileName).toString();
				destination = new File(new StringBuilder(movieNameNoExt).append(ext).toString());
			}
			
			LOG.info("Parent :" + source.getParent());
			
			final File parent = source.getParentFile();
			final List<File> subtitles = FileUtils.getFileForMatch(parent, FileUtils.SUBPATTERN);
			File subFile;
			for (final File subtitle : subtitles) {
				if (subtitles.size() == 1 || subtitle.getName().contains("english") || subtitle.getName().contains("eng")) {
					LOG.info("Subtitle file found");
					subFile = new File(new StringBuilder(movieNameNoExt).append(".en").append(subtitle.getName().substring(subtitle.getName().lastIndexOf('.'))).toString());
					subtitle.renameTo(subFile);
				}
			}
			
		}

		source.renameTo(destination);
		
		FileUtils.checkEmptyParent(source, this.source, false);

		LOG.info("Old File Name :" + source.getAbsolutePath());
		LOG.info("New File Name :" + destination.getAbsoluteFile());

		ProcessUtils.updateDatebase(destination.getAbsolutePath(), fileTypeStr);
		ProcessUtils.checkSendEmail(exchange, source, destination.getAbsolutePath());
		ProcessUtils.sendNotifications("Finished File '" + destination.getName() + "' Sorted as a '" + fileTypeStr + "'");
	}

}
