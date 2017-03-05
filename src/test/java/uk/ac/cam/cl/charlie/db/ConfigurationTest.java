package uk.ac.cam.cl.charlie.db;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test for class {@link Configuration}
 *
 * @author Gary Guo
 */
public class ConfigurationTest {

	private Configuration config;

	public ConfigurationTest() {
		config = Configuration.getInstance();

		// Delete entries to avoid previous tests' result
		config.delete("test");
		config.delete("test1");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testGetKeyNotNull() {
		config.get(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPutKeyNotNull() {
		config.put(null, "TEST");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testPutValueNotNull() {
		config.put("test", null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDeleteKeyNotNull() {
		config.delete(null);
	}

	@Test
	public void test() {
		// get should return null for non-existence entries
		assertNull(config.get("test"));
		assertNull(config.get("test1"));

		// put should update the entry without affecting others
		config.put("test", "TEST");
		assertEquals("TEST", config.get("test"));
		assertNull(config.get("test1"));

		config.put("test1", "TEST1");
		assertEquals("TEST", config.get("test"));
		assertEquals("TEST1", config.get("test1"));

		// delete should remove the entry without affecting others
		config.delete("test");
		assertNull(config.get("test"));
		assertEquals("TEST1", config.get("test1"));
	}
}
