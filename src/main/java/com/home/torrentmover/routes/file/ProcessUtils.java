package com.home.torrentmover.routes.file;

import java.io.File;
import java.util.Arrays;
import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;

import org.apache.camel.Exchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.home.torrentmover.SpringStart;
import com.home.torrentmover.model.FileType;
import com.home.torrentmover.model.ProcessedFile;

public class ProcessUtils {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessUtils.class);

	public static void updateDatebase(final String destination, final String fileTypeStr) {
		final EntityManager em = SpringStart.getEntityManager();

		if (em != null) {
			em.getTransaction().begin();

			FileType fileType;
			try {
				fileType = FileType.findWithName(em, fileTypeStr);
			} catch (NoResultException e) {
				LOG.warn("No Existing File Type :" + fileTypeStr);
				fileType = new FileType();
				fileType.setType(fileTypeStr);
				em.persist(fileType);
			}

			ProcessedFile procFile;
			try {
				procFile = ProcessedFile.findWithName(em, destination);
			} catch (NoResultException e) {
				LOG.warn("No Existing File with Name :" + destination);
				procFile = new ProcessedFile();
				em.persist(fileType);
				procFile.setFileName(destination);
			}
			procFile.setDateProcessed(new Date());
			procFile.setFileType(fileType);

			em.persist(procFile);
			em.getTransaction().commit();
		}
	}

	public static void checkSendEmail(final Exchange exchange, final File source, final String destination) {
		final String email = SpringStart.prop.getProperty("email.use");
		if (email != null && "true".equals(email)) {
			final String result = EmailMessageFormatter.getHTMLMessage(Arrays
					.asList("</br>From :<p>" + source.getAbsolutePath() + "</p></br>To :<p>" + destination + "</p>"));
			exchange.getOut().setBody(result);
			exchange.getOut().setHeader("myMail", email);
		}
	}
}
