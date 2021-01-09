package brere.nat.torrentmover.routes.file;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtils {

	private static final long SLEEPTIME = 1000L;
	private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

	public static final String EPISODEREGEX = "[Ss]\\d{1,}[Ee]\\d{1,}";
	public static final String MOVIEREGEX = ".{1,}\\d{4}([^p])";
	public static final String SUBREGEX = ".*\\.srt|.*\\.smi|.*\\.ssa|.*\\.ass|.*\\.vtt";
	public static final String YEARREGEX = "\\d{4}";
	public static final String SEASONREGEX = "[Ss]\\d{1,}";

	public static final Pattern EPISODEPATTERN = Pattern.compile(EPISODEREGEX);
	public static final Pattern MOVIEPATTERN = Pattern.compile(MOVIEREGEX, Pattern.MULTILINE);
	public static final Pattern SUBPATTERN = Pattern.compile(SUBREGEX);
	public static final Pattern YEARPATTERN = Pattern.compile(YEARREGEX);
	public static final Pattern SEASONPATTERN = Pattern.compile(SEASONREGEX);
	private static final ExtensionFilter EXT_FILTER = new ExtensionFilter();

	/**
	 * 
	 * @param files
	 * @param source
	 */
	public static void emptyParent(final File[] files, final String source) {
		for (final File file : files) {
			LOG.info("File To Check :" + file);
			if (file != null) {
				if (file.isDirectory() && file.listFiles().length > 0) {
					LOG.info("Is not empty Dir");
					if (file.getName().equalsIgnoreCase("sample")) {
						LOG.info("Folder is called Sample, deleting");
						deleteSampleFile(file);
					}
					emptyParent(file.listFiles(), source);
				}
				if (!file.getAbsolutePath().equals(source)
						&& !(file.getAbsolutePath() + File.separatorChar).equals(source) && !EXT_FILTER.check(file.getName())) {
					LOG.info("Deleting :" + file.getAbsoluteFile());
					file.delete();
				}
			}
		}
	}

	/**
	 * 
	 * @param file
	 */
	private static void deleteSampleFile(final File file) {
		for (File sampleFile : file.listFiles()) {
			if (sampleFile.isDirectory()) {
				deleteSampleFile(sampleFile);
			} else {
				sampleFile.delete();
			}
		}
	}

	/**
	 * 
	 * @param in
	 * @return
	 */
	public static String setFirstCharUppercase(final String in) {
		return in.substring(0, 1).toUpperCase() + in.substring(1, in.length());
	}

	/**
	 * 
	 * @param in
	 * @return
	 */
	public static String removeSpecialChars(final String in) {
		return in.replace('.', ' ').replace('_', ' ').replaceAll("\\[(.*?)\\]", "").replaceAll("\\((.*?)\\)", "")
				.trim();
	}

	/**
	 * 
	 * @param body
	 * @return
	 * @throws InterruptedException
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static File checkFile(final String body) throws InterruptedException, FileNotFoundException, IOException {
		File source = new File(body);
		final Date started = new Date();
		final DateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
		while (!source.canWrite()) {
			LOG.info("Source file is locked, waiting until it's unlocked.");
			LOG.info("Started checking :" + df.format(started));
			Thread.sleep(SLEEPTIME);
			source = new File(body);
			if (!source.exists()) {
				throw new FileNotFoundException("File '" + source.getCanonicalPath() + File.pathSeparator + source.getName() + "' Doesn't exist anymore");
			}
		}
		LOG.info("Source file unlocked, running process");
		return source;
	}

	/**
	 * 
	 * @param fileName
	 * @param uppercase
	 * @return
	 */
	public static String getMovieName(final String fileName, final boolean uppercase) {
		String newFileName;
		if (uppercase) {
			newFileName = setFirstCharUppercase(removeSpecialChars(fileName));
		} else {
			newFileName = removeSpecialChars(fileName);
		}
		LOG.info("Starting Matching on :" + newFileName);
		final Matcher matches = MOVIEPATTERN.matcher(newFileName);
		if (matches.find()) {
			MatchResult matchRes = matches.toMatchResult();
			LOG.info("First Match :" + matchRes.group());
			while (matches.find(matches.end() -1)) {
				matchRes = matches.toMatchResult();
				LOG.info("Another Match :" + matchRes.group());
			}
			final String match = matchRes.group();
			LOG.info("Found Year String :" + match);
			newFileName = match.trim();
			LOG.info("New File Name :" + newFileName);
		}
		return newFileName;
	}

	/**
	 * 
	 * @param fileName
	 * @param uppercase
	 * @return
	 */
	public static String getSeriesName(final String fileName, final boolean uppercase) {
		String seriesName;
		if (uppercase) {
			seriesName = setFirstCharUppercase(removeSpecialChars(fileName.split(EPISODEREGEX)[0]));
		} else {
			seriesName = removeSpecialChars(fileName.split(EPISODEREGEX)[0]);
		}
		return seriesName;
	}

	/**
	 * 
	 * @param source
	 * @param sourceStr
	 * @param ftp
	 */
	public static void checkEmptyParent(final File source, final String sourceStr, final boolean ftp) {
		final File parentDir = source.getParentFile();
		if (ftp) {
			source.delete();
		}
		LOG.info("Parent Dir :" + parentDir.getAbsoluteFile());
		final File[] files = parentDir.listFiles(EXT_FILTER);
		LOG.info("Number of Files :" + files.length);
		
		if (files.length == 0) {
			File[] fileArr = new File[parentDir.listFiles().length + 1];
			fileArr[parentDir.listFiles().length] = parentDir;
			emptyParent(fileArr, sourceStr);
			LOG.info("Parent DIR :" + parentDir.getAbsolutePath());
			LOG.info("Source DIR :" + sourceStr);
			LOG.info("Source DIR :" + sourceStr.substring(0, sourceStr.length() - 1));
			if (!parentDir.getAbsolutePath().equals(sourceStr) && !parentDir.getAbsolutePath().equals(sourceStr.substring(0, sourceStr.length() - 1))) {
				parentDir.delete();
			}
		}
	}
	
	/**
	 * 
	 * @param nameToCheck
	 * @param dirToCheck
	 * @return
	 */
	public static File getExistingFolder(String nameToCheck, final File dirToCheck) {
		File seriesFolderFound = null;
		for (final File dir : dirToCheck.listFiles()) {
			if (dir.isDirectory()
					&& (dir.getName().equalsIgnoreCase(nameToCheck) || dir.getName().contains(nameToCheck))) {
				seriesFolderFound = dir;
			}
		}
		return seriesFolderFound;
	}
	
	/**
	 * 
	 * @param dir
	 * @param regex
	 * @return
	 */
	public static List<File> getFileForMatch(final File dir, final Pattern regex) {
		List<File> files = new ArrayList<>();
		Matcher match;
		for (final File file : dir.listFiles()) {
			match = regex.matcher(file.getName());
			if (file.isDirectory()) {
				files.addAll(getFileForMatch(file, regex));
			} else if (match.find()) {
				files.add(file);
			}
		}
		return files;
	}
	
	/**
	 * 
	 * @param fileName
	 * @return
	 */
	public static String getSeasonName(final String fileName) {
		Matcher match = SEASONPATTERN.matcher(fileName);
		final String season;
		if (match.find()) {
			season = match.group().toLowerCase().replace("s", "Season ");
		} else {
			season = "";
		}
		return season;
	}
	
	/**
	 * 
	 * @param file
	 */
	public static void createDir(final File file) {
		if (!file.exists()) {
			file.mkdirs();
			file.setExecutable(true, false);
			file.setReadable(true, false);
			file.setWritable(true, false);
		}
	}
}
