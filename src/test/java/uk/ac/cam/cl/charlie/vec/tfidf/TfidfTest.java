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
        }

    }

}
