package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.ArrayList;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.mail.Messages;
import uk.ac.cam.cl.charlie.vec.tfidf.BasicWordCounter;

/**
 * Given a cluster of emails generates and sets the name for the cluster based
 * on the sender of the emails in that cluster
 *
 * @author M Boyce
 */
public class SenderNamer extends ClusterNamer {

    private static double MIN_PROPORTION_CORRECT = 0.8;
    private static double MAX_CONFIDENCE = 0.9;

    @Override
    public NamingResult name(Cluster<Message> cluster) {

        ArrayList<ClusterableObject<Message>> messages = cluster.getObjects();

        // Map storing number of occurrences of each domain name in the sender
        // address of the messages in the cluster
        BasicWordCounter counter = new BasicWordCounter();

        for (int i = 0; i < messages.size(); i++) {
            try {
                InternetAddress from = Messages.getFromAddress(messages.get(i).getObject());
                String[] address = from.getAddress().split("@");

                // Remove the .com /.net ect
                // String[] tempDomain = address[1].split("\\.");
                // String domain = "";

                counter.increment(address[1]);
                //
                // for (int j = 0; j < tempDomain.length - 1; j++) {
                // domain += tempDomain[j];
                // }
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        String domain = counter.words().stream()
                // Sort by descending frequency
                .sorted((w1, w2) -> counter.frequency(w2) - counter.frequency(w1))
                // Get first element
                .findFirst().get();

        // Test to see if is a good name for cluster by seeing what proportion
        // of the Cluster this name holds for
        double confidence = (double) counter.frequency(domain) / messages.size();
        if (confidence < MIN_PROPORTION_CORRECT) {
            return null;
        }
        return new NamingResult(domain, confidence * MAX_CONFIDENCE);
    }

}
