package com.home.torrentmover.routes.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.file.GenericFile;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.home.torrentmover.SpringStart;

public class FTPFileMoverProcessor implements Processor {

	private static final String EPISODEREGEX = "[Ss]\\d{1,}[Ee]\\d{1,}";
	private static final String MOVIEREGEX = "( \\d{4} )";
	private static final Logger LOG = LoggerFactory.getLogger(FTPFileMoverProcessor.class);

	private static final Pattern EPISODEPATTERN = Pattern.compile(EPISODEREGEX);
	private static final Pattern MOVIEPATTERN = Pattern.compile(MOVIEREGEX);
	private final String movies;
	private final String series;
	private final String source;

	public FTPFileMoverProcessor() {
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
		String destination = null;
		
		final FTPClient ftp = new FTPClient();
		final FTPClientConfig config = new FTPClientConfig();
		config.setDefaultDateFormatStr("dd-MM-yyyy hh:mm:ss");

		ftp.configure(config);
		
		ftp.connect(SpringStart.prop.getProperty("ftp.host"));
		ftp.login(SpringStart.prop.getProperty("ftp.user"), SpringStart.prop.getProperty("ftp.password"));
		
		if (nameMatcher.find()) {
			String seriesName = fileName.split(EPISODEREGEX)[0].replace('.', ' ').replace('_', ' ')
					.replaceAll("\\[(.*?)\\]", "").replaceAll("\\((.*?)\\)", "").trim();
			
			final FTPFile[] seriesDirs = ftp.listDirectories(series);
			LOG.info("Series Name :" + seriesName);
			FTPFile seriesFolderFound = null;
			for (final FTPFile dir : seriesDirs) {
				if (dir.getName().equalsIgnoreCase(seriesName)) {
					seriesFolderFound = dir;
					seriesName = dir.getName();
				}
			}
			if (seriesFolderFound == null) {
				
				LOG.info("Series :" + series + seriesName);
				
				ftp.makeDirectory(series + seriesName);
				
				destination = series + seriesName + "/" + seriesName + " " + nameMatcher.group().toUpperCase() + ext;
			} else {
				seriesName = seriesName.substring(0, 1).toUpperCase() + seriesName.substring(1);
				LOG.info("Found Series Folder :" + seriesName);
				destination = series + "/" + seriesFolderFound.getName() + "/" + seriesName + " "
						+ nameMatcher.group().toUpperCase() + ext;
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
			destination = movies + newFileName + ext;
		}
		try (final FileInputStream fis = new FileInputStream(source)){
		
			ftp.storeFile(destination, fis);
		
		} catch (final Exception e) {
			e.printStackTrace();
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
		
		LOG.info("Old File Name :" + source.getAbsolutePath());
		LOG.info("New File Name :" + destination);

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