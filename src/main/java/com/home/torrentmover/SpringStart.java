package com.home.torrentmover;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringStart {

	private static final Logger LOG = LoggerFactory.getLogger(SpringStart.class);
	public static Properties prop;

	public static void main(String[] args) {
		try (InputStream input = new FileInputStream("application.properties")) {
			prop = new Properties();
			prop.load(input);
		} catch (Exception e) {
			LOG.error("Exception when loading Properties");
		}
		SpringApplication.run(SpringStart.class, args);
	}

}
