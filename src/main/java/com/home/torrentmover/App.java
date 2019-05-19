package com.home.torrentmover;

public class App {
	public static void main(String[] args) {
		final String source;
		final String movies;
		final String series;
		
		if (args.length > 0) {
			source = args[0];
		} else {
			source = "/drives/storage/Videos/Downloads/";
		}
		if (args.length > 1) {
			movies = args[1];
		} else {
			movies = "/drives/storage/Videos/Movies/";
		}
		if (args.length > 2) {
			series = args[2];
		} else {
			series = "/drives/storage/Videos/Series/";
		}
		
		new CamelStarter(source, movies, series).run();
	}
}
