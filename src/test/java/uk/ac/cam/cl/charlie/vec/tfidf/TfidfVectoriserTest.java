package uk.ac.cam.cl.charlie.vec.tfidf;

import org.junit.Test;
import uk.ac.cam.cl.charlie.vec.TextVector;

import java.util.Optional;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;

public class TfidfVectoriserTest {
	
	private TfidfVectoriser tfidf;

	public TfidfVectoriserTest() {
		tfidf = new TfidfVectoriser();
	}
	
	@Test
	public void testModelLoad() {
	    try {
            tfidf.load();
		    assertEquals(true, tfidf.ready());
		    tfidf.close();
        } catch (Error e) {
            fail();
        }
        finally {
	        System.gc();
        }

	}

	@Test
    public void testWord2Vec() {
        tfidf.load();

        assertNotSame(Optional.empty(), tfidf.word2vec("hello"));
		// Check nothing bad happens with null strings or empty strings
        tfidf.word2vec("");

        try {
            tfidf.close();
        } catch (Error e) {
            fail();
        }
        finally {
            System.gc();
        }
    }

    @Test(expected = NullPointerException.class)
    public void testWord2VecNull () {
        tfidf.load();
        tfidf.word2vec(null);
        try {
            tfidf.close();
        } catch (Error e) {
            fail();
        }
        finally {
            System.gc();
        }
    }

    // todo add tests for the doc2vec functionality

}
