package com.home.torrentmover.routes.file;

import java.io.File;
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
			File seriesFolderFound = null;
			for (final File dir : seriesDir.listFiles()) {
				if (dir.isDirectory()
						&& (dir.getName().equalsIgnoreCase(seriesName) || dir.getName().contains(seriesName))) {
					seriesFolderFound = dir;
					seriesName = dir.getName();
				}
			}
			if (seriesFolderFound == null) {
				final File newDir = new File(series + seriesName);
				newDir.mkdir();
				destination = new File(newDir.getAbsolutePath() + File.separatorChar + seriesName + " "
						+ nameMatcher.group().toUpperCase() + ext);
			} else {
				LOG.info("Found Series Folder");
				destination = new File(seriesFolderFound.getAbsolutePath() + File.separatorChar + seriesName + " "
						+ nameMatcher.group().toUpperCase() + ext);
			}
		} else {
			fileTypeStr = MOVIE;
			String newFileName = FileUtils.getMovieName(fileName, uppercase);
			final Matcher matches = FileUtils.MOVIEPATTERN.matcher(newFileName);
			if (matches.find()) {
				LOG.info("Found Year String");
				newFileName = (newFileName.split(FileUtils.MOVIEREGEX)[0] + matches.group()).trim();
				LOG.info("New File Name :" + newFileName);
			}
			destination = new File(movies + newFileName + ext);
		}

		source.renameTo(destination);
		
		FileUtils.checkEmptyParent(source, this.source, false);

		LOG.info("Old File Name :" + source.getAbsolutePath());
		LOG.info("New File Name :" + destination.getAbsoluteFile());

		ProcessUtils.updateDatebase(destination.getAbsolutePath(), fileTypeStr);
		ProcessUtils.checkSendEmail(exchange, source, destination.getAbsolutePath());
	}

}
