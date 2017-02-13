package uk.ac.cam.cl.charlie.vec.tfidf;

import org.junit.Test;
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
            tf = Tfidf.getInstance();
        } catch (SQLException e) {
            e.printStackTrace();
        }
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

    public void checkClose() {
        try {
            tf = Tfidf.getInstance();

            tf.totalNumberDocuments(); // quick call to check the database is loaded

            tf.close();
            tf.close(); // shouldn't do anything bad

        } catch (SQLException e) {
            fail();
        }

    }

}
