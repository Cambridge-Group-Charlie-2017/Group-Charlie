package uk.ac.cam.cl.charlie.vec.tfidf;

import java.sql.*;
import java.util.HashMap;
import java.util.TreeSet;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.vec.Document;

import javax.print.Doc;

/**
 * Created by shyam on 05/02/2017. Edited by LP 05/02/2017
 */
public final class Tfidf {
    // Presumably this will have to be stored in the database (which I will leave for later).
    // Use singleton pattern to stop subclassing

    private static Tfidf instance = null;
    private Database database; // the database instance should store the word frequencies.

    // TODO note I strongly disagree with this class having:
    // a) any tables (the data has to be persisted to a database
    // b) being used by external classes except TfidfVectoriser

    // TODO this implementation with the database

    // TODO I would also suggest aiming for very low coupling with the vectorising class
    // there are other uses for tfidf ;)

    private Tfidf() throws SQLException {
        database = Database.getInstance();
        Connection conn = database.getConnection();
        DatabaseMetaData metaData = conn.getMetaData();
        ResultSet checkTableExistenceResult = metaData.getTables(null, null, "WORD_FREQUENCIES", null);
        if (checkTableExistenceResult.next()) {
            // there exists a table in this database with word frequencies
            return;
        }

        else {
            // need to create the database table
            String sql = "CREATE TABLE WORD_FREQUENCIES(word VARCHAR(50) NOT NULL,count INTEGER NOT NULL)";
            Statement stmt = conn.createStatement();
            stmt.execute(sql);

            // insert a value to represent the number of documents in total:
            sql = "INSERT INTO WORD_FREQUENCIES(word,count) VALUES (totalNumberOfDocs,0)";
            stmt.execute(sql);

            stmt.close();
            conn.commit();
            conn.close();
        }
    }

    public static Tfidf getInstance() throws SQLException {
        if (instance == null) {
            instance = new Tfidf();
        }
        return instance;
    }

    public int totalNumberDocuments() {
        return 0;
    }
    
    public int numberOfDocsWithWith(String word) {
        return 0;
    }
    
    public void addDocument(Document doc) {
        // TODO
    }
    
    public void addDocument(Document doc, HashMap<String, Integer> wordfrequency) {
        // To be used when wordfrequency has already been calculated
    }

    public void addDocumentToCounts(Document doc) throws SQLException {
        if (database == null) {
            database = Database.getInstance();
        }

        Connection conn = database.getConnection();
        Statement sqlstmt = conn.createStatement();

        // check the existence of a the table
    }

}
