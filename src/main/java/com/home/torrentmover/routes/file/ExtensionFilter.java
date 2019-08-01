package com.home.torrentmover.routes.file;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.List;

import com.home.torrentmover.SpringStart;

public class ExtensionFilter implements FilenameFilter {

	public boolean accept(File dir, String name) {
		final String lowercaseName = name.substring(name.length() - name.lastIndexOf('.')).toLowerCase();
		final List<String> extensions = Arrays.asList(SpringStart.prop.getProperty("file.formats").split("|"));
		if (extensions.contains(lowercaseName)) {
			return true;
		} else {
			return false;
		}
	}

}
