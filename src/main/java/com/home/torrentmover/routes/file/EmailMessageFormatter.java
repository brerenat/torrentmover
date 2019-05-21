package com.home.torrentmover.routes.file;

import java.util.List;

public class EmailMessageFormatter {

	
	public static String getHTMLMessage(final List<String> thingsDone) {
		final StringBuilder message = new StringBuilder();
		message.append("<html><body><p>Hi Nathan,</p></br></br><p>We've successfully processed these files:</p></br></br>");
		for (final String thing : thingsDone) {
			message.append("<p>").append(thing).append("</p></br>");
		}
		message.append("<p>Regards</p><p>Camel</p></body></html>");
		
		return message.toString();
	}
}
