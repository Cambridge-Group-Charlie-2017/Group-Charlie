package uk.ac.cam.cl.charlie.clustering;

import org.junit.Test;
import weka.clusterers.EM;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Vector;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Created by Ben on 06/02/2017.
 */
public class EMClusteringTest {

    @Test
    public void mainTest() throws Exception{
        EMClusterer em = new EMClusterer();


        Session sess = Session.getDefaultInstance(new Properties());

        ArrayList<Message> messages = new ArrayList<Message>();
        for (int i = 0; i < 150; i++) {
            messages.add(new MimeMessage(sess));
        }
        em.evalClusters(messages);
        ArrayList<Cluster> clusters = em.getClusters();

        // perform various tests on 'clusters'
        assertNotNull(clusters);
        assertNotEquals(clusters.size(), 0);

        int bestCluster = Integer.MAX_VALUE;
        double bestMatch = Double.MAX_VALUE;
        Vector<Double> vec = testGetVec();

        for (int i = 0; i < clusters.size(); i++) {
            double currMatch = clusters.get(i).matchStrength(vec);
            if (currMatch < bestMatch) {
                bestMatch = currMatch;
                bestCluster = i;
            }
        }
        EMCluster cl = (EMCluster) clusters.get(bestCluster);

        //cl.addMessage(null);


    }

    public static Vector<Double> testGetVec() {

        int dimensionality = 300;
        Vector<Double> v = new Vector<Double>();
        for (int j = 0; j < dimensionality; j++) {
            v.add(java.lang.Math.random() * 4);
        }

        return v;
    }

}
