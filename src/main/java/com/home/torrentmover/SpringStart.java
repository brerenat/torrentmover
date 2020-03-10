package com.home.torrentmover;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SpringStart {

	private static final Logger LOG = LoggerFactory.getLogger(SpringStart.class);
	private static Properties prop;
	private static EntityManager em;

	public static Properties getProp() {
		return prop;
	}

	public static void setProp(Properties prop) {
		SpringStart.prop = prop;
	}

	public static EntityManager getEm() {
		return em;
	}

	public static void setEm(EntityManager em) {
		SpringStart.em = em;
	}
	
	public static void main(String[] args) {

		try (final InputStream input = new FileInputStream("application.properties")) {
			prop = new Properties();
			prop.load(input);

			Object obj = prop.get("database.use");
			LOG.info("Obj :" + obj.getClass());

			if (obj == null || "true".contentEquals(obj.toString())) {
				EntityManagerFactory factory = Persistence.createEntityManagerFactory("torrentmover", prop);
				em = factory.createEntityManager();
			}

		} catch (Exception e) {
			LOG.error("Exception when loading Properties");
		}

		SpringApplication.run(SpringStart.class, args);
	}

}
