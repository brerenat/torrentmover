package com.home.torrentmover.routes.file;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

	private static final long SLEEPTIME = 1000L;
	private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

	public static final String EPISODEREGEX = "[Ss]\\d{1,}[Ee]\\d{1,}";
	public static final String MOVIEREGEX = ".*\\d{4}([^p])";

	public static final Pattern EPISODEPATTERN = Pattern.compile(EPISODEREGEX);
	public static final Pattern MOVIEPATTERN = Pattern.compile(MOVIEREGEX);

	public static void emptyParent(final File[] files, final String source) {
		for (final File file : files) {
			LOG.info("File To Check :" + file);
			if (file != null) {
				if (file.isDirectory() && file.listFiles().length > 0) {
					LOG.info("Is not empty Dir");
					emptyParent(file.listFiles(), source);
				}
				if (!file.getAbsolutePath().equals(source)
						&& !(file.getAbsolutePath() + File.separatorChar).equals(source)) {
					LOG.info("Deleting :" + file.getAbsoluteFile());
					file.delete();
				}
			}
		}
	}

	public static String setFirstCharUppercase(final String in) {
		return in.substring(0, 1).toUpperCase() + in.substring(1, in.length());
	}

	public static String removeSpecialChars(final String in) {
		return in.replace('.', ' ').replace('_', ' ').replaceAll("\\[(.*?)\\]", "").replaceAll("\\((.*?)\\)", "")
				.trim();
	}

	public static File checkFile(final String body) throws InterruptedException {
		File source = new File(body);
		final Date started = new Date();
		final DateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
		while (!source.canWrite()) {
			LOG.info("Source file is locked, waiting until it's unlocked.");
			LOG.info("Started checking :" + df.format(started));
			Thread.sleep(SLEEPTIME);
			source = new File(body);
		}
		LOG.info("Source file unlocked, running process");
		return source;
	}

	public static String getMovieName(final String fileName, final boolean uppercase) {
		String newFileName;
		if (uppercase) {
			newFileName = setFirstCharUppercase(removeSpecialChars(fileName));
		} else {
			newFileName = removeSpecialChars(fileName);
		}
		final Matcher matches = MOVIEPATTERN.matcher(newFileName);
		if (matches.find()) {
			LOG.info("Found Year String");
			newFileName = matches.group().trim();
			LOG.info("New File Name :" + newFileName);
		}
		return newFileName;
	}

	public static String getSeriesName(final String fileName, final boolean uppercase) {
		String seriesName;
		if (uppercase) {
			seriesName = setFirstCharUppercase(removeSpecialChars(fileName.split(EPISODEREGEX)[0]));
		} else {
			seriesName = removeSpecialChars(fileName.split(EPISODEREGEX)[0]);
		}
		return seriesName;
	}

	public static void checkEmptyParent(final File source, final String sourceStr, final boolean ftp) {
		final File parentDir = source.getParentFile();
		if (ftp) {
			source.delete();
		}
		LOG.info("Parent Dir :" + parentDir.getAbsoluteFile());
		final File[] files = parentDir.listFiles(new ExtensionFilter());
		LOG.info("Number of Files :" + files.length);
		if (files.length == 0) {
			File[] fileArr = new File[parentDir.listFiles().length + 1];
			fileArr[parentDir.listFiles().length] = parentDir;
			emptyParent(fileArr, sourceStr);
			if (!parentDir.getAbsolutePath().equals(sourceStr)) {
				parentDir.delete();
			}
		}
	}
}
