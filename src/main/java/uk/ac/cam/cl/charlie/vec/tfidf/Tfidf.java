package uk.ac.cam.cl.charlie.vec.tfidf;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.vec.Document;

/**
 * Created by shyam on 05/02/2017. Edited by LP 05/02/2017
 */
public final class Tfidf {
    // Presumably this will have to be stored in the database (which I will leave for later).
    // Use singleton pattern to stop subclassing
    private static String TOTAL_NUMBER_OF_DOCS = "totalnumberofdocstfidf";

    private static String GET_SQL = "SELECT freq FROM WORD_FREQUENCIES WHERE word = ?";
    private static String INSERT_SQL = "INSERT INTO WORD_FREQUENCIES VALUES (?, ?)";

    // no clue if this works
    private static String UPDATE_SQL = "UPDATE WORD_FREQUENCIES SET freq = ? WHERE word = ?";

    private static Tfidf instance = null;
    private Database database; // the database instance should store the word frequencies.
    private PreparedStatement insertStmt;
    private PreparedStatement getStmt;
    private PreparedStatement updateStmt;

    private boolean isClosed;

    private Tfidf() throws SQLException {
        database = Database.getInstance();


        if (!database.tableExists("WORD_FREQUENCIES")) {
            // need to create the database table
            String sql = "CREATE TABLE WORD_FREQUENCIES(word VARCHAR(50) NOT NULL,freq INTEGER NOT NULL)";
            database.executeUpdate(sql);
            insertStmt = database.getConnection().prepareStatement(INSERT_SQL);

            // insert a value to represent the number of documents in total:
            insertStmt.setString(1, TOTAL_NUMBER_OF_DOCS);
            insertStmt.setInt(2, 0);
            insertStmt.executeUpdate(sql);
        }
        else {
            insertStmt = database.getConnection().prepareStatement(INSERT_SQL);
        }

        getStmt = database.getConnection().prepareStatement(GET_SQL);

        updateStmt = database.getConnection().prepareStatement(UPDATE_SQL);

        isClosed = false;

    }

    public static Tfidf getInstance() throws SQLException {
        if (instance == null) {
            instance = new Tfidf();
        } else if (instance.isClosed()) {
            instance = new Tfidf();
        }
        return instance;
    }

    public int totalNumberDocuments() throws SQLException {
        // just a special case of getCount
        return numberOfDocsWithWith(TOTAL_NUMBER_OF_DOCS);
    }
    
    public int numberOfDocsWithWith(String word) throws SQLException {
        getStmt.setString(1, word);
        try (ResultSet rs = getStmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1); // check this line
            } else {
                return 0;
            }
        }
    }

    // The overloaded function has been deleted (deliberately) - all calls should come through here, and here alone
    public void addDocument(Document doc) throws TfidfException {
        // Generate a hashmap with all keys in lowercase, then update database

        Map<String, Integer> wordCounts = new HashMap<>();

        String text = doc.getContent();
        for (String w : text.split("[\\W]")) {
            w = w.toLowerCase(); // all our keys are in lower case
            if (wordCounts.containsKey(w)) {
                wordCounts.put(w, wordCounts.get(w) + 1); // autoboxed
            }
            else {
                wordCounts.put(w, 1);
            }
        }
        wordCounts.remove("");

        // iterate over the keys, and call incrementWordBy
        for (String word : wordCounts.keySet()) {
            try {
                incrementWordBy(word, wordCounts.get(word));
            } catch (SQLException e) {
                throw new Error(e);
            } catch (TfidfException e) {
                continue; // just ignore the word, for now (perhaps do something else in the future)
            }

        }

        try {
            incrementWord(TOTAL_NUMBER_OF_DOCS);
        } catch (SQLException e) {
            throw new TfidfException();
        }
    }
    
    public void incrementWord(String word) throws SQLException, TfidfException {
        incrementWordBy(word, 1);
    }

    public void incrementWordBy(String word, int n) throws SQLException, TfidfException {

        getStmt.setString(1, word);
        ResultSet rs = getStmt.executeQuery();

        if (rs.next()) {
            // the word is already in the database
            int count = rs.getInt(1);
            updateStmt.setInt(1, count + n);
            updateStmt.setString(2, word);
            updateStmt.execute();
        }

        else {
            insertStmt.setString(1, word);
            insertStmt.setInt(2, 1); // autoincrement to 1
            insertStmt.execute();
        }

        rs.close();
    }

    public void close() throws SQLException {
        // do all the cleaning up
        updateStmt.close();
        getStmt.close();
        insertStmt.close();
    }

    public boolean isClosed() {
        return isClosed;
    }
}
