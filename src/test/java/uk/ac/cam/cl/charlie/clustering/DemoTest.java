package uk.ac.cam.cl.charlie.clustering;

import org.junit.Test;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Created by Ben on 11/02/2017.
 */
public class DemoTest {

    @Test
    public void mainTest() throws Exception{
        GenericEMClusterer em = new GenericEMClusterer();

        Session sess = Session.getDefaultInstance(new Properties());

        ArrayList<ClusterableObject> messages = new ArrayList<ClusterableObject>();
        for (int i = 0; i < 250; i++) {
            messages.add(new ClusterableMessage(new MimeMessage(sess)));
        }


        ArrayList<ArrayList<DemoMessageVector>> vecs = em.demoClusterer(messages);

        System.out.println("First vec: "+vecs.get(0).get(0).vec.toString());

    }
}
