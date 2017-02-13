package uk.ac.cam.cl.charlie.vec.tfidf;

import org.junit.Test;
import uk.ac.cam.cl.charlie.vec.TextVector;

import javax.mail.Message;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.sql.SQLException;
import java.util.*;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;

public class TfidfVectoriserTest {
	
	private TfidfVectoriser tfidf;

	public TfidfVectoriserTest() {
        tfidf = TfidfVectoriser.getVectoriser();

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

        HashSet<Message> messages = new HashSet<Message>();

        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFOUR! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIVE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nI am writing to inform you of a fire that has broken out on the premesis " +
              "of 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nRoy Trenneman.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "Denham@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nRegards,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Brick street.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 167 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to seeing you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nEagerly anticipating your arrival." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Hello sir/madam,\n\nI came here to drink milk, and kick ass. And I've just finished my milk." +
              "\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nl8rz mate." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "Tech problems",
           "Dear sir/madam,\n\nHave you tried turning it off and on again?" +
              "\n\nNow leave me alone,\nRoy.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "neg1@countdown.co.uk",
           "Street Countdown",
           "Dear negative one,\n\nGood morning, that's a nice tnettenba\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nWord.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "Peter.file@gmail.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Hello Peter,\n\nI'm sorry for the confusion that arose over your name, and for the offence I caused." +
              "\n\nI hope you can forgive me." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));


	    //Print out subject and body content, ensure messages are constructed successfully.
        System.out.println("Subject: "+((Message)(messages.toArray()[0])).getSubject());
        MimeMultipart content = (MimeMultipart)((Message)(messages.toArray()[0])).getContent();
        String body;
        if (content.getCount() > 0)
            body = (String)content.getBodyPart(0).getContent();
        else {
            fail();
            return;
        }
        System.out.println(body);

        //Test the vectorisation.
       // tfidf.load();
        Set<TextVector> vecs = tfidf.doc2vec(messages);
        double[] components = ((TextVector)vecs.toArray()[0]).getRawComponents();
        //tfidf.close();
        assertNotNull(components);
        //System.out.println(Arrays.toString(components));
    }

    // todo add tests for the doc2vec functionality

}
