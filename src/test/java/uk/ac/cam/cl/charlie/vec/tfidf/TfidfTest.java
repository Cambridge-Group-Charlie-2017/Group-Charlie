package uk.ac.cam.cl.charlie.vec.tfidf;

import org.junit.Test;
import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.vec.Document;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Created by shyam on 08/02/2017.
 */
public class TfidfTest {
    private Tfidf tf;

    public TfidfTest () {
        try {
            wipe();
            tf = Tfidf.getInstance();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void wipe() {
        Database d = Database.getInstance();
        if (d.tableExists("WORD_FREQUENCIES")) {
            // drop table
            try {
                d.getConnection().prepareStatement("DROP TABLE WORD_FREQUENCIES").execute();
            } catch (SQLException e) {
                e.printStackTrace();
                System.err.println("Couldn't drop word frequencies table");
            }
        }
        String sql = "CREATE TABLE WORD_FREQUENCIES(word VARCHAR(50) NOT NULL,freq INTEGER NOT NULL)";
        d.executeUpdate(sql);
    }

    @Test
    public void checkInit() {
        try {
            tf = Tfidf.getInstance();
        } catch (SQLException e) {
            fail();
        }
    }

    @Test
    public void checkDocuments() {
        if (tf == null) {
            checkInit();
        }

        try {
            int nDocs = tf.totalNumberDocuments();
            Document d = new Document("file.txt", "This is an example sentence for the unit test of the db");
            tf.addDocument(d);
            assertEquals(nDocs + 1, tf.totalNumberDocuments());
        } catch (SQLException e) {
            fail();
        } catch (TfidfException e) {
            fail();
        } finally {
        }

    }

    @Test
    public void checkAddWord() {
        if (tf == null) {
            checkInit();
        }

        String testWord = "Hello";
        try {
            int count = tf.numberOfDocsWithWith(testWord);
            tf.incrementWord(testWord);
            assertEquals(count + 1, tf.numberOfDocsWithWith(testWord));
            tf.incrementWordBy(testWord, 3);
            assertEquals(count + 4, tf.numberOfDocsWithWith(testWord));

        } catch (SQLException e) {
            fail();
        } catch (TfidfException e) {
            fail();
        }


    }

    @Test
    public void checkTotalIdempotent() {
        try {
            assertEquals(tf.totalNumberDocuments(), tf.totalNumberDocuments());
        } catch (SQLException e) {
            fail();
        }
    }
}
