package uk.ac.cam.cl.charlie.clustering;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;
import org.junit.Test;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import static org.junit.Assert.*;

/**
 * Created by Ben on 01/02/2017.
 */
public class KMeansClustererTest {
    //TODO: use pre-loaded vectors, output clusters.

    /*
    * invoke evalClusters() and then getClusters() on an instance of KMeansClusterer.
    * Because vectoriser isn't implemented, evalClusters() won't actually group the messages properly.
    * Instead, it uses a 150-large predefined test set of vectors, stored in iris-vector.arff.
    *
    * When a new Message is added to a cluster, because vectoriser isn't implemented, it generates a
    * random vector for that message.
    *
    * All vectors used for test purposes are 4 dimensional.
    */



    @Test public void mainTest() throws Exception{
        KMeansClusterer km = new KMeansClusterer();


        Session sess = Session.getDefaultInstance(new Properties());

        ArrayList<Message> messages = new ArrayList<Message>();
        for (int i = 0; i < 150; i++) {
            messages.add(new MimeMessage(sess));
        }
        km.evalClusters(messages);
        ClusterGroup clusters = km.getClusters();

        // perform various tests on 'clusters'
        assertNotNull(clusters);
        assertNotEquals(clusters.size(), 0);

        int bestCluster = Integer.MAX_VALUE;
        double bestMatch = Double.MAX_VALUE;
        Vector<Double> vec = new Vector<Double>();
        vec.add(1.0);
        vec.add(1.0);
        vec.add(1.0);
        vec.add(1.0);
        for (int i = 0; i < clusters.size(); i++) {
            double currMatch = clusters.get(i).matchStrength(messages.get(i));
            if (currMatch < bestMatch) {
                bestMatch = currMatch;
                bestCluster = i;
            }
        }
        KMeansCluster cl = (KMeansCluster) clusters.get(bestCluster);
        Vector<Double> prevCentroid = (Vector<Double>) cl.getCentroid().clone();
        cl.addMessage(null);
        Vector<Double> currCentroid = cl.getCentroid();
        assertNotEquals(prevCentroid, currCentroid);

    }



}
