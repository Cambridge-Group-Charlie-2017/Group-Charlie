package uk.ac.cam.cl.charlie.db;

/**
 * Centralized persistent configuration manager
 *
 * @author Gary Guo
 */
public class Configuration {

    private static Configuration instance;

    private PersistentMap<String, String> map;

    private Configuration() {
	Database db = Database.getInstance();

	map = db.getMap("config", Serializers.STRING, Serializers.STRING);
    }

    /**
     * Get the singleton instance of Configuration object.
     *
     * @return instance of Configuration
     */
    public static Configuration getInstance() {
	if (instance == null) {
	    instance = new Configuration();
	}
	return instance;
    }

    /**
     * Returns the value of the configuration entry associated with the
     * specified key, or {@code null} if no entry with the specified key exists.
     *
     * @param key
     *            the key whose associated value is to be returned
     * @return the value associated with the specified key, or {@code null} if
     *         the configuration entry is non-existent
     */
    public String get(String key) {
	return map.get(key);
    }

    /**
     * Associate a value with the specified key in the configuration. Previous
     * value associated with the specified key (if any) will be overridden.
     * Parameter value cannot be null, to avoid accidental deletion of
     * configuration entries. To delete an entry explicitly, use
     * {@link #delete(String)}.
     *
     * @param key
     *            the key whose associated configuration value will be set
     * @param value
     *            the value to be associated with the key, must not be
     *            {@code null}
     */
    public void put(String key, String value) {
	map.put(key, value);
    }

    /**
     * Remove the configuration entry with the specified key in the
     * configuration. Previous value associated with the specified key (if any)
     * will be deleted.
     *
     * @param key
     *            the key whose associated configuration entry will be removed
     */
    public void delete(String key) {
	map.remove(key);
    }

}
