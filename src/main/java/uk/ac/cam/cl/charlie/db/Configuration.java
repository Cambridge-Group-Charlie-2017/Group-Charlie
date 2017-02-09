package uk.ac.cam.cl.charlie.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Centralized persistent configuration manager
 *
 * @author Gary Guo
 */
public class Configuration {

	// SQL statement for creating config table
	private static final String CREATE_CONFIG_TABLE = "CREATE TABLE config "
			+ "(key VARCHAR(255) NOT NULL PRIMARY KEY, value VARCHAR(65535) NOT NULL)";

	// SQL statements for select, update and delete entries
	private static final String SELECT_CONFIG = "SELECT value FROM config WHERE key = ?";
	private static final String UPDATE_CONFIG = "MERGE INTO config USING (VALUES(?, ?)) "
			+ "AS vals(x, y) ON config.key = vals.x "
			+ "WHEN MATCHED THEN UPDATE SET config.value = vals.y "
			+ "WHEN NOT MATCHED THEN INSERT VALUES vals.x, vals.y";
	private static final String DELETE_CONFIG = "DELETE FROM config WHERE key = ?";

	private static final Logger log = LoggerFactory
			.getLogger(Configuration.class);

	private static Configuration instance;

	private Database db;
	private PreparedStatement selectStmt;
	private PreparedStatement updateStmt;
	private PreparedStatement deleteStmt;

	private Configuration() {
		db = Database.getInstance();

		// Create the table if is not created
		if (!db.tableExists("config")) {
			log.info("Table config does not exist, creating...");
			db.executeUpdate(CREATE_CONFIG_TABLE);
			log.info("Table config created");
		} else {
			log.info("Table config exists already");
		}

		// Initialize prepared statements
		Connection conn = db.getConnection();
		try {
			selectStmt = conn.prepareStatement(SELECT_CONFIG);
			updateStmt = conn.prepareStatement(UPDATE_CONFIG);
			deleteStmt = conn.prepareStatement(DELETE_CONFIG);
		} catch (SQLException e) {
			log.error("Failed to create prepared statement", e);
			throw new Error(e);
		}
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
		if (key == null) {
			throw new IllegalArgumentException("Key must not be null");
		}

		try {
			selectStmt.setString(1, key);
			try (ResultSet set = selectStmt.executeQuery()) {
				if (set.next()) {
					return set.getString(1);
				} else {
					return null;
				}
			}
		} catch (SQLException e) {
			log.error("Failed to retrieve configuration", e);
			throw new Error(e);
		}
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
		if (key == null) {
			throw new IllegalArgumentException("Key must not be null");
		}

		// This is a design choice to prevent entries from being accidentally
		// deleted when value is null
		if (value == null) {
			throw new IllegalArgumentException(
					"Value must not be null. To delete a configuration entry, use delete() instead");
		}

		try {
			updateStmt.setString(1, key);
			updateStmt.setString(2, value);
			updateStmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Failed to update configuration", e);
			throw new Error(e);
		}
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
		if (key == null) {
			throw new IllegalArgumentException("Key must not be null");
		}

		try {
			deleteStmt.setString(1, key);
			deleteStmt.executeUpdate();
		} catch (SQLException e) {
			log.error("Failed to remove configuration", e);
			throw new Error(e);
		}
	}

}
