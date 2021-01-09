package brere.nat.torrentmover.routes.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brere.nat.torrentmover.SpringStart;

public class ExtensionFilter implements FilenameFilter {
	private static final Logger LOG = LoggerFactory.getLogger(ExtensionFilter.class);
	private final List<String> extensions;
	
	public ExtensionFilter() {
		extensions = new ArrayList<>();
		for (final String ext : SpringStart.getProp().getProperty("file.formats").split("\\|")) {
			extensions.add(ext.substring(ext.lastIndexOf('.')));
		}
	}

	public boolean accept(final File dir, final String name) {
		return check(name);
	}
	
	public boolean check(final String name) {
		LOG.info("File name :" + name);
		final boolean result;
		if (name.contains(".")) {
			final String lowercaseName = name.substring(name.lastIndexOf('.')).toLowerCase();
			LOG.info("Extension :" + lowercaseName);
			LOG.info("Extensions :" + extensions);
			if (extensions.contains(lowercaseName)) {
				result = true;
			} else {
				result = false;
			}
		} else {
			result = false;
		}
		return result;
	}
}