package com.home.torrentmover.routes.file;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.home.torrentmover.SpringStart;

@Component
public class FileMover extends RouteBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(FileMover.class);

	@Override
	public void configure() throws Exception {
		LOG.info("Starting Configure");
		if (SpringStart.getProp().getProperty("ftp.user") == null || SpringStart.getProp().getProperty("ftp.user").isEmpty()) {
			from("file:{{source}}?recursive=true&noop=true&include={{file.formats}}").process(new FileMoverProcessor())
					.choice().when(header("myMail").isEqualTo("true"))
					.to("smtps://{{email.smtp}}:{{email.port}}?username={{email.sender}}&password={{email.password}}&from={{email.sender}}&to={{email.to}}&subject={{email.subject}}&contentType=text/html")
					.otherwise().endChoice().end();
		} else {
			from("file:{{source}}?recursive=true&noop=true&include={{file.formats}}").process(new FTPFileMoverProcessor())
			.end();
		}
	}

}
