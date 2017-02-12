package uk.ac.cam.cl.charlie.clustering;

import org.deeplearning4j.clustering.cluster.Cluster;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Properties;



/**
 * Created by Ben on 06/02/2017.
 */
public class EMClusteringTest {

    @Test
    public void mainTest() throws Exception{
        GenericEMClusterer em = new GenericEMClusterer();


        Session sess = Session.getDefaultInstance(new Properties());

        ArrayList<ClusterableObject> messages = new ArrayList<ClusterableObject>();
        for (int i = 0; i < 250; i++) {
            messages.add(new ClusterableMessage(new MimeMessage(sess)));
        }

        em.evalClusters(messages);

        GenericClusterGroup clusters = em.getClusters();
        System.out.println("Number of clusters: " + clusters.size());

        // perform various tests on 'clusters'
        assertNotNull(clusters);
        assertNotEquals(clusters.size(), 0);


        int bestCluster = Integer.MAX_VALUE;
        double bestMatch = 0;
        ClusterableObject msg = messages.get(110);

        for (int i = 0; i < clusters.size(); i++) {
            double currMatch = clusters.get(i).matchStrength(msg);
            if (currMatch > bestMatch) {
                bestMatch = currMatch;
                bestCluster = i;
            }
        }
        //clusters.get(bestCluster).addMessage(msg);

        //Assert.assertEquals(2, bestCluster);
        System.out.println("Belongs in cluster " + bestCluster);

        //if that's the best cluster, it should already be there.
        assertTrue(clusters.get(bestCluster).contains(msg));
    }
}
