package com.home.torrentmover.routes.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.home.torrentmover.SpringStart;

public class FileMoverProcessor implements Processor {

	private static final String EPISODEREGEX = "[Ss]\\d{1,}[Ee]\\d{1,}";
	private static final String MOVIEREGEX = "( \\d{4} )";
	private static final Logger LOG = LoggerFactory.getLogger(FileMoverProcessor.class);

	private static final Pattern EPISODEPATTERN = Pattern.compile(EPISODEREGEX);
	private static final Pattern MOVIEPATTERN = Pattern.compile(MOVIEREGEX);
	private final String movies;
	private final String series;
	private final String source;

	public FileMoverProcessor() {
		super();
		this.movies = SpringStart.prop.getProperty("movies");
		this.series = SpringStart.prop.getProperty("series");
		this.source = SpringStart.prop.getProperty("source");
	}

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
			String seriesName = fileName.split(EPISODEREGEX)[0].replace('.', ' ').replace('_', ' ')
					.replaceAll("\\[(.*?)\\]", "").replaceAll("\\((.*?)\\)", "").trim();
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
				destination = new File(
						newDir.getAbsolutePath() + "/" + seriesName + " " + nameMatcher.group().toUpperCase() + ext);
				source.renameTo(destination);
			} else {
				LOG.info("Found Series Folder");
				destination = new File(seriesFolderFound.getAbsolutePath() + "/" + seriesName + " "
						+ nameMatcher.group().toUpperCase() + ext);
				source.renameTo(destination);
			}
		} else {
			String newFileName = fileName.replace('.', ' ').replace('_', ' ').replaceAll("\\[(.*?)\\]", "")
					.replaceAll("\\((.*?)\\)", "");
			final Matcher matches = MOVIEPATTERN.matcher(newFileName);
			if (matches.find()) {
				LOG.info("Found Year String");
				newFileName = (newFileName.split(MOVIEREGEX)[0] + matches.group()).trim();
				LOG.info("New File Name :" + newFileName);
			}
			destination = new File(movies + newFileName + ext);
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

		final String result = EmailMessageFormatter.getHTMLMessage(Arrays.asList("</br>From :<p>"
				+ source.getAbsolutePath() + "</p></br>To :<p>" + destination.getAbsolutePath() + "</p>"));
		exchange.getOut().setBody(result);
	}

	private void emptyParent(final File[] files) {
		for (final File file : files) {
			LOG.info("File To Check :" + file);
			if (file != null) {
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
