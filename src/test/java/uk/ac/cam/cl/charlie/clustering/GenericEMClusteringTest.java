package uk.ac.cam.cl.charlie.clustering;

import org.junit.Test;
import uk.ac.cam.cl.charlie.vec.tfidf.*;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;

import static org.junit.Assert.*;


/**
 * Created by Ben on 06/02/2017.
 */
public class GenericEMClusteringTest {

    @Test
    public void mainTest() throws Exception{
        GenericEMClusterer em = new GenericEMClusterer();

        ArrayList<File> files = new ArrayList<File>();
        HashSet<Message> messages = new HashSet<Message>();

        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFOUR! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIVE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nI am writing to inform you of a fire that has broken out on the premesis " +
              "of 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nRoy Trenneman.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "Denham@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nRegards,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Brick street.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 167 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to seeing you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nEagerly anticipating your arrival." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Hello sir/madam,\n\nI came here to drink milk, and kick ass. And I've just finished my milk." +
              "\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nl8rz mate." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "Tech problems",
           "Dear sir/madam,\n\nHave you tried turning it off and on again?" +
              "\n\nNow leave me alone,\nRoy.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "neg1@countdown.co.uk",
           "Street Countdown",
           "Dear negative one,\n\nGood morning, that's a nice tnettenba\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nWord.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "Peter.file@gmail.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Hello Peter,\n\nI'm sorry for the confusion that arose over your name, and for the offence I caused." +
              "\n\nI hope you can forgive me." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));
        messages.add(uk.ac.cam.cl.charlie.vec.tfidf.MessageCreator.createMessage(
           "fire@01189998819991197253.co.uk",
           "moss.m@reynholm.co.uk",
           "FIRE",
           "Dear sir/madam,\n\nFIRE! FIRE! HELP ME! 123 Carenden Road.\n\nLooking forward to hearing from you." +
              "\n\nAll the best,\nMaurice Moss.",
           files
        ));

        ArrayList<Message> messageList = new ArrayList<>(messages);
        em.evalClusters(messageList);

        GenericClusterGroup clusters = em.getClusters();
        System.out.println("Number of clusters: " + clusters.size());

        // perform various tests on 'clusters'
        assertNotNull(clusters);
        assertNotEquals(clusters.size(), 0);


        int bestCluster = Integer.MAX_VALUE;
        double bestMatch = 0;
        Message msg =  messageList.get(110);

        for (int i = 0; i < clusters.size(); i++) {
            double currMatch = clusters.get(i).matchStrength(new ClusterableMessage(msg));
            if (currMatch > bestMatch) {
                bestMatch = currMatch;
                bestCluster = i;
            }
        }
        //clusters.get(bestCluster).addMessage(msg);

        //Assert.assertEquals(2, bestCluster);
        System.out.println("Belongs in cluster " + bestCluster);

        //if that's the best cluster, it should already be there.
        assertTrue(clusters.get(bestCluster).containsMessage(msg));
    }
}
