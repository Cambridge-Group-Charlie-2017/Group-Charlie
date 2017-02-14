package uk.ac.cam.cl.charlie.clustering;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Test;

/**
 * Created by Ben on 11/02/2017.
 */
public class DemoTest {

    @Test
    public void mainTest() throws Exception {
	GenericEMClusterer em = new GenericEMClusterer();

	Session sess = Session.getDefaultInstance(new Properties());

	ArrayList<ClusterableMessage> messages = new ArrayList<>();
	for (int i = 0; i < 250; i++) {
	    messages.add(new ClusterableMessage(new MimeMessage(sess)));
	}

	ArrayList<ArrayList<DemoMessageVector>> vecs = em.demoClusterer(messages);

	System.out.println("First vec: " + (Arrays.toString(vecs.get(0).get(0).vec)));

    }
}
