package uk.ac.cam.cl.charlie.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

	private static final String TABLE_EXISTS = "SELECT * FROM INFORMATION_SCHEMA.TABLES "
			+ "WHERE TABLE_SCHEMA = 'PUBLIC' AND UPPER(TABLE_NAME) = ?";

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

	public static void panic(Exception e) {
		log.error("Error when accessing the database", e);
		throw new Error(e);
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

	/**
	 * Utility function for checking table existence.
	 *
	 * @param tableName
	 *            name of the table to check
	 * @return {@code true} if the table exists, or {@code false} if it does not
	 */
	public boolean tableExists(String tableName) {
		try {
			try (PreparedStatement statement = conn
					.prepareStatement(TABLE_EXISTS)) {
				statement.setString(1, tableName.toUpperCase());
				try (ResultSet set = statement.executeQuery()) {
					if (set.next()) {
						return true;
					} else {
						return false;
					}
				}
			}
		} catch (SQLException e) {
			panic(e);
			return false;
		}
	}

	/**
	 * Execute a statement (with optional parameters). Please make sure that the
	 * statement is syntactically correct and valid number of arguments is
	 * supplied, since failure of execution of this method is a fatal error. Do
	 * not use this method for a frequently used update. In such case, please
	 * use {@link #getConnection()} and
	 * {@link Connection#prepareStatement(String)}.
	 *
	 * @param stmt
	 *            the statement to execute, use "?" for parameters
	 * @param args
	 *            the argument to be supplied to the statement
	 * @return update count
	 */
	public int executeUpdate(String stmt, String... args) {
		try (PreparedStatement statement = conn.prepareStatement(stmt)) {
			for (int i = 0; i < args.length; i++) {
				statement.setString(i + 1, args[i]);
			}
			return statement.executeUpdate();
		} catch (SQLException e) {
			panic(e);
			return 0;
		}
	}

}
