package uk.ac.cam.cl.charlie.vec.tfidf;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import uk.ac.cam.cl.charlie.db.Database;

/**
 * Class for accessing word counter information stored in the database.
 *
 * @author Gary Guo
 */
public class PersistantWordCounter extends WordCounter {

    private static String GET_SQL = "SELECT freq FROM WORD_FREQUENCIES WHERE word = ?";
    private static String INSERT_SQL = "INSERT INTO WORD_FREQUENCIES VALUES (?, ?)";
    private static String UPDATE_SQL = "UPDATE WORD_FREQUENCIES SET freq = ? WHERE word = ?";

    private PreparedStatement getStmt;
    private PreparedStatement insertStmt;
    private PreparedStatement updateStmt;

    private static PersistantWordCounter instance = null;
    private Database database;

    private PersistantWordCounter() {
	database = Database.getInstance();

	// Create table if it does not exist
	if (!database.tableExists("WORD_FREQUENCIES")) {
	    String sql = "CREATE TABLE WORD_FREQUENCIES(word VARCHAR(50) NOT NULL,freq INTEGER NOT NULL)";
	    database.executeUpdate(sql);
	}

	try {
	    getStmt = database.getConnection().prepareStatement(GET_SQL);
	    insertStmt = database.getConnection().prepareStatement(INSERT_SQL);
	    updateStmt = database.getConnection().prepareStatement(UPDATE_SQL);
	} catch (SQLException e) {
	    throw new Error(e);
	}
    }

    public static PersistantWordCounter getInstance() {
	if (instance == null) {
	    instance = new PersistantWordCounter();
	}
	return instance;
    }

    @Override
    public void increment(String word, int n) {
	// Word length limitation
	if (word.length() > 50)
	    return;

	try {
	    boolean exists = false;
	    int count = 0;

	    // First fetch current value
	    getStmt.setString(1, word);
	    try (ResultSet set = getStmt.executeQuery()) {
		if (set.next()) {
		    exists = true;
		    count = set.getInt(1);
		}
	    }

	    // Update or insert accordingly
	    if (exists) {
		updateStmt.setString(2, word);
		updateStmt.setInt(1, count + n);
		updateStmt.executeUpdate();
	    } else {
		insertStmt.setString(1, word);
		insertStmt.setInt(2, n);
		insertStmt.executeUpdate();
	    }
	} catch (SQLException e) {
	    throw new Error(e);
	}

    }

    @Override
    public int frequency(String word) {
	// Word length limitation
	if (word.length() > 50)
	    return 0;

	try {
	    getStmt.setString(1, word);
	    try (ResultSet set = getStmt.executeQuery()) {
		if (set.next()) {
		    return set.getInt(1);
		} else {
		    return 0;
		}
	    }
	} catch (SQLException e) {
	    throw new Error(e);
	}
    }

    @Override
    public Set<String> words() {
	throw new UnsupportedOperationException("Querying all words from a persisted word counter is too expensive");
    }

}
