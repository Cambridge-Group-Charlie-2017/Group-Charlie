package uk.ac.cam.cl.charlie.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.swing.JFrame;

import org.hsqldb.util.DatabaseManagerSwing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.charlie.util.OS;

/**
 * Class for connecting to the database
 *
 * @author Gary Guo
 */
public class Database {

	private static Database instance;
	private static final Logger log = LoggerFactory.getLogger(Database.class);

	private Connection conn;

	private Database() {
		try {
			Class.forName("org.hsqldb.jdbcDriver");
		} catch (ClassNotFoundException e) {
			log.error("Failed to load HSQL database driver", e);
			throw new Error(e);
		}

		// Compute the path of the database
		String path = OS.getAppDataDirectory("AutoArchive");
		String dbpath = path + File.separator + "db";
		String dburl = "jdbc:hsqldb:file:" + dbpath;

		log.info("Database located at {}", dburl);

		try {
			conn = DriverManager.getConnection(dburl, "SA", "");
		} catch (SQLException e) {
			log.error("Failed to connect to the database", e);
			throw new Error(e);
		}
	}

	/**
	 * Get the singleton instance of Database object.
	 *
	 * @return instance of Database
	 */
	public static Database getInstance() {
		if (instance == null) {
			instance = new Database();
		}
		return instance;
	}

	/**
	 * Launch HSQL's built-in database manager UI.
	 */
	public static void launchDatabaseManagerUI() {
		log.info("Starting database manager UI");

		// DatabaseManagerSwing will close the connection when it closes
		// to keep our connection open, we need to create a new instance
		Database db = new Database();
		DatabaseManagerSwing m = new DatabaseManagerSwing(
				new JFrame("HSQL Database Manager"));
		m.main();
		m.connect(db.getConnection());
		m.start();
	}

	/**
	 * Get the JDBC connection.
	 *
	 * @return instance of JDBC connection
	 */
	public Connection getConnection() {
		return conn;
	}

}
