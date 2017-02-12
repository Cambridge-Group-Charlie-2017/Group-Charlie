package uk.ac.cam.cl.charlie.vec.tfidf;

import org.junit.Test;
import uk.ac.cam.cl.charlie.vec.TextVector;

import javax.mail.Message;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;

public class TfidfVectoriserTest {
	
	private TfidfVectoriser tfidf;

	public TfidfVectoriserTest() {
	    try {
            tfidf = new TfidfVectoriser();
        } catch (SQLException e) {}

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


    @Test
    public void testMessage2Vec() throws Exception{
        ArrayList<File> files = new ArrayList<File>();
	    Message msg = MessageCreator.createMessage(
	       "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
           );


	    //Print out subject and body content, ensure messages are constructed successfully.
        System.out.println("Subject: "+msg.getSubject());
        MimeMultipart content = (MimeMultipart)msg.getContent();
        String body;
        if (content.getCount() > 0)
            body = (String)content.getBodyPart(0).getContent();
        else {
            fail();
            return;
        }
        System.out.println(body);

        //Test the vectorisation.
        tfidf.load();
        double[] components = tfidf.doc2vec(msg).getRawComponents();
        tfidf.close();
        assertNotNull(components);
        System.out.println(Arrays.toString(components));
    }

    // todo add tests for the doc2vec functionality

}
