package brere.nat.torrentmover;

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

import brere.nat.mydb.utils.ProcessUtils;

@SpringBootApplication
public class SpringStart {

	private static final Logger LOG = LoggerFactory.getLogger(SpringStart.class);
	private static Properties prop;

	public static Properties getProp() {
		return prop;
	}

	public static void setProp(Properties prop) {
		SpringStart.prop = prop;
	}

	public static EntityManager getEm() {
		return ProcessUtils.getEm();
	}

	public static void main(String[] args) {

		try (final InputStream input = new FileInputStream("application.properties")) {
			prop = new Properties();
			prop.load(input);

			Object dbUse = prop.get("database.use");

			LOG.info("DB Use :"+ dbUse);
			if (dbUse != null && "true".contentEquals(dbUse.toString())) {
				EntityManagerFactory factory = Persistence.createEntityManagerFactory("torrentmover", prop);
				ProcessUtils.setEm(factory.createEntityManager());
			}

		} catch (Exception e) {
			LOG.error("Exception when loading Properties", e);
		}

		SpringApplication.run(SpringStart.class, args);
		LOG.info("Started Spring Application");
		
	}

}
