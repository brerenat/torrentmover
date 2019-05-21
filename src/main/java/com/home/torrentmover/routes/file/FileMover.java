package com.home.torrentmover.routes.file;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileMover extends RouteBuilder {

	private static final Logger LOG = LoggerFactory.getLogger(FileMover.class);

	@Override
	public void configure() throws Exception {
		LOG.info("Starting Configure");
		from("file:{{source}}?recursive=true&noop=true&include={{file.formats}}").process(new FileMoverProcessor()).to(
				"smtps://{{email.smtp}}?username={{email.sender}}&password={{email.password}}&from={{email.sender}}&to={{email.to}}&subject={{email.subject}}&contentType=text/html")
				.end();
	}

}
