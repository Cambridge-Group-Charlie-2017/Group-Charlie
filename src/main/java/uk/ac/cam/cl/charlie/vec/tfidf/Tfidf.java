package uk.ac.cam.cl.charlie.vec.tfidf;

import java.sql.*;
import java.util.HashMap;
import java.util.TreeSet;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.vec.Document;

/**
 * Created by shyam on 05/02/2017. Edited by LP 05/02/2017
 */
public final class Tfidf {
    // Presumably this will have to be stored in the database (which I will leave for later).
    // Use singleton pattern to stop subclassing
    private static String INSERT_SQL = "INSERT INTO WORD_FREQUENCIES VALUES (?, ?)";
    private static String TOTAL_NUMBER_OF_DOCS = "";

    private static String GET_SQL = "SELECT freq FROM WORD_FREQUENCIES WHERE word = ?";

    private static Tfidf instance = null;
    private Database database; // the database instance should store the word frequencies.
    private PreparedStatement insertStmt;
    private PreparedStatement getStmt;

    // TODO note I strongly disagree with this class having:
    // a) any tables (the data has to be persisted to a database
    // b) being used by external classes except TfidfVectoriser

    // TODO this implementation with the database

    // TODO I would also suggest aiming for very low coupling with the vectorising class
    // there are other uses for tfidf ;)

    private Tfidf() throws SQLException {
        database = Database.getInstance();

        insertStmt = database.getConnection().prepareStatement(INSERT_SQL);

        getStmt = database.getConnection().prepareStatement(GET_SQL);

        if (!database.tableExists("WORD_FREQUENCIES")) {
            // need to create the database table
            String sql = "CREATE TABLE WORD_FREQUENCIES(word VARCHAR(50) NOT NULL,freq INTEGER NOT NULL)";
            database.executeUpdate(sql);

            // insert a value to represent the number of documents in total:
            insertStmt.setString(1, TOTAL_NUMBER_OF_DOCS);
            insertStmt.setInt(2, 0);
            insertStmt.executeUpdate(sql);
        }
    }

    public static Tfidf getInstance() throws SQLException {
        if (instance == null) {
            instance = new Tfidf();
        }
        return instance;
    }

    public int totalNumberDocuments() throws SQLException {
        getStmt.setString(1, TOTAL_NUMBER_OF_DOCS);
        ResultSet rs = getStmt.executeQuery();
        if (rs.next()) {
            return rs.getInt(1); // check this line
        }

        else {
            throw new SQLException();
        }
    }
    
    public int numberOfDocsWithWith(String word) throws SQLException {
        getStmt.setString(1, word);
        ResultSet rs = getStmt.executeQuery();
        if (rs.next()) {
            return rs.getInt(1); // check this line
        }

        else {
            throw new SQLException();
        }
    }

    public void addCountToWord(String word) {

    }
    
    public void addDocument(Document doc) {
        // Generate a hashmap with all keys in lowercase, then update database

    }
    
    public void addDocument(Document doc, HashMap<String, Integer> wordfrequency) {
        // To be used when wordfrequency has already been calculated
    }

}
