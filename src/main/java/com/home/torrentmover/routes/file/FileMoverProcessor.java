package com.home.torrentmover.routes.file;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.home.mydb.utils.ProcessUtils;
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
			destination = processSeries(nameMatcher, ext, fileName, uppercase);
		} else {
			fileTypeStr = MOVIE;
			destination = processMovie(source, ext, fileName, uppercase);
			
		}

		source.renameTo(destination);
		
		FileUtils.checkEmptyParent(source, this.source, false);

		LOG.info("Old File Name :" + source.getAbsolutePath());
		LOG.info("New File Name :" + destination.getAbsoluteFile());

		ProcessUtils.updateDatebase(destination.getAbsolutePath(), fileTypeStr);
	}

	/**
	 * 
	 * @param nameMatcher
	 * @param ext
	 * @param fileName
	 * @param uppercase
	 * @return
	 */
	private File processSeries(final Matcher nameMatcher, final String ext, final String fileName,
			final boolean uppercase) {
		final File destination;
		final String seriesName = FileUtils.getSeriesName(fileName, uppercase);
		final File seriesDir = new File(series);
		LOG.info("Series Name :" + seriesName);
		File seriesFolder = FileUtils.getExistingFolder(seriesName, seriesDir);
		final String seasonAndEpisodeNumber = nameMatcher.group();
		if (seriesFolder == null) {
			seriesFolder = new File(series + seriesName);
			FileUtils.createDir(seriesFolder);
			LOG.info("Created Series Folder");
		} else {
			LOG.info("Found Series Folder");
		}
		destination = getSeasonDestination(ext, seriesName, seriesFolder, seasonAndEpisodeNumber);
		return destination;
	}

	/**
	 * 
	 * @param source
	 * @param ext
	 * @param fileName
	 * @param uppercase
	 * @return
	 */
	private File processMovie(final File source, final String ext, final String fileName, final boolean uppercase) {
		final File destination;
		LOG.info("File Name :" + fileName);
		String movieName = FileUtils.getMovieName(fileName, uppercase);
		LOG.info("New File Name :" + movieName);
		final Matcher matches = FileUtils.YEARPATTERN.matcher(movieName);
		if (matches.find()) {
			LOG.info("Found Year String");
			movieName = new StringBuilder(movieName.split(FileUtils.YEARREGEX)[0]).append("(").append(matches.group()).append(")").toString().trim();
			LOG.info("New File Name :" + movieName);
		}
		
		final File moviesDir = new File(movies);
		File moviesFolder = FileUtils.getExistingFolder(movieName, moviesDir);
		
		
		if (moviesFolder == null) {
			moviesFolder = new File(movies + movieName);
			FileUtils.createDir(moviesFolder);
			LOG.info("Created Movies Folder");
		} else {
			LOG.info("Found Movies Folder");
		}
		
		final String movieNameNoExt = new StringBuilder(moviesFolder.getAbsolutePath()).append(File.separatorChar).append(movieName).toString();
		destination = new File(new StringBuilder(movieNameNoExt).append(ext).toString());
		
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
		return destination;
	}

	/**
	 * 
	 * @param ext
	 * @param seriesName
	 * @param seriesFolder
	 * @param seasonAndEpisodeNumber
	 * @return
	 */
	private File getSeasonDestination(final String ext, final String seriesName, File seriesFolder,
			final String seasonAndEpisodeNumber) {
		final File destination;
		final String season = FileUtils.getSeasonName(seasonAndEpisodeNumber);
		LOG.info("Season :" + season);
		File seasonFolder = FileUtils.getExistingFolder(season, seriesFolder);
		if (seasonFolder == null) {
			seasonFolder = new File(new StringBuilder(seriesFolder.getAbsolutePath()).append(File.separatorChar).append(season).toString());
			FileUtils.createDir(seasonFolder);
			
			destination = new File(new StringBuilder(seasonFolder.getAbsolutePath()).append(File.separatorChar).append(seriesName)
					.append(" ").append(seasonAndEpisodeNumber.toUpperCase()).append(ext).toString());
			
		} else {
			destination = new File(new StringBuilder(seasonFolder.getAbsolutePath()).append(File.separatorChar).append(seriesName)
					.append(" ").append(seasonAndEpisodeNumber.toUpperCase()).append(ext).toString());
		}
		return destination;
	}

}
