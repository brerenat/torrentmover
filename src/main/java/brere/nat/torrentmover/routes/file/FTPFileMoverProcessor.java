package brere.nat.torrentmover.routes.file;

import java.io.File;
import java.io.FileInputStream;
import java.util.regex.Matcher;

import org.apache.camel.Exchange;
import org.apache.camel.component.file.GenericFile;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brere.nat.mydb.utils.ProcessUtils;
import brere.nat.torrentmover.SpringStart;

public class FTPFileMoverProcessor extends AbstractFileMoverProcessor {

	private static final Logger LOG = LoggerFactory.getLogger(FileMoverProcessor.class);

	public FTPFileMoverProcessor() {
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
		final String destination;
		final String fileTypeStr;

		final FTPClient ftp = new FTPClient();
		final FTPClientConfig config = new FTPClientConfig();
		config.setDefaultDateFormatStr("dd-MM-yyyy hh:mm:ss");

		ftp.configure(config);

		ftp.connect(SpringStart.getProp().getProperty("ftp.host"));
		ftp.login(SpringStart.getProp().getProperty("ftp.user"), SpringStart.getProp().getProperty("ftp.password"));

		final boolean uppercase = Boolean.getBoolean(SpringStart.getProp().getProperty("file.uppercase.firstchar"));

		if (nameMatcher.find()) {
			fileTypeStr = SERIES;
			String seriesName = FileUtils.getSeriesName(fileName, uppercase);

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
				LOG.info("Found Series Folder :" + seriesName);
				destination = series + "/" + seriesFolderFound.getName() + "/" + seriesName + " "
						+ nameMatcher.group().toUpperCase() + ext;
			}
		} else {
			fileTypeStr = MOVIE;
			destination = movies + FileUtils.getMovieName(fileName, uppercase) + ext;
		}
		try (final FileInputStream fis = new FileInputStream(source)) {

			ftp.storeFile(destination, fis);

		} catch (final Exception e) {
			e.printStackTrace();
		}

		FileUtils.checkEmptyParent(source, this.source, true);

		LOG.info("Old File Name :" + source.getAbsolutePath());
		LOG.info("New File Name :" + destination);

		ProcessUtils.updateDatebase(destination, fileTypeStr);
	}

}
