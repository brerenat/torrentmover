package com.home.torrentmover;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory.Client;
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

		try (final InputStream input = new FileInputStream("startup.properties")) {
			prop = new Properties();
			prop.load(input);

			Object dbUse = prop.get("database.use");

			LOG.info("DB Use :"+ dbUse);
			if (dbUse != null && "true".contentEquals(dbUse.toString())) {
				EntityManagerFactory factory = Persistence.createEntityManagerFactory("torrentmover", prop);
				em = factory.createEntityManager();
			}

		} catch (Exception e) {
			LOG.error("Exception when loading Properties", e);
		}

		SpringApplication.run(SpringStart.class, args);
		LOG.info("Started Spring Application");
		
		try {
			Server server = new Server();
			server.setHandler(new HomepageHandler());

			HttpConfiguration https = new HttpConfiguration();
			https.addCustomizer(new SecureRequestCustomizer());
			
			
			// Jetty Basic Server
			Client client = new Client();
			client.setKeyStorePath(SpringStart.class.getResource("/keystore").toExternalForm());
			client.setKeyStorePassword("changeit");
			client.setKeyManagerPassword("changeit");
			ServerConnector sslConnector = new ServerConnector(server,
					new SslConnectionFactory(client, "http/1.1"), new HttpConnectionFactory(https));
			sslConnector.setPort(443);
			server.setConnectors(new Connector[] { sslConnector });

			server.start();
			server.join();
		} catch (Exception e) {
			LOG.error("Exception when Starting Jetty server", e);
		}
		
	}

}
