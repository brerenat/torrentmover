package brere.nat.torrentmover.routes.track;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SeriesTrackerProcessorTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(SeriesTrackerProcessorTest.class);
	private static SeriesTrackerProcessor processor;
	

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		processor = new SeriesTrackerProcessor();
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@Test
	void updateSeasonMapFromFileSystem() {
		try {
			processor.updateSeasonMapFromFileSystem("Z:\\Videos\\Series\\Vikings");
		} catch (Exception e) {
			LOG.error(e.getMessage(), e);
			fail("updateSeasonMapFromFileSystem");
		}
	}

}
