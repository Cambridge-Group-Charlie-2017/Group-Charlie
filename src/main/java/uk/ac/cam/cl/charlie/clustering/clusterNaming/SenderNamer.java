package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.mail.Messages;

/**
 * Given a cluster of emails generates and sets the name for the cluster based
 * on the sender of the emails in that cluster
 *
 * @author M Boyce
 */
public class SenderNamer extends ClusterNamer {

    private static double MIN_PROPORTION_CORRECT = 0.5;

    @Override
    public NamingResult name(Cluster<Message> cluster) {

        ArrayList<ClusterableObject<Message>> messages = cluster.getObjects();

        // Map storing number of occurrences of each domain name in the sender
        // address of the messages in the cluster
        TreeMap<String, Integer> domains = new TreeMap<>();

        for (int i = 0; i < messages.size(); i++) {
            try {
                InternetAddress from = Messages.getFromAddress(messages.get(i).getObject());
                String[] address = from.getAddress().split("@");

                // Remove the .com /.net ect
                String[] tempDomain = address[1].split("\\.");
                String domain = "";

                for (int j = 0; j < tempDomain.length - 1; j++) {
                    domain += tempDomain[j];
                }

                // Increment the count of relevant domain
                int count = 0;
                if (domains.containsKey(domain)) {
                    count = domains.get(domain);
                }
                domains.put(domain, count + 1);

            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        // Test to see if is a good name for cluster by seeing what proportion
        // of the Cluster this name holds for
        Map.Entry<String, Integer> mostCommonDomain = domains.lastEntry();
        double confidence = mostCommonDomain.getValue() / messages.size();
        if (confidence < MIN_PROPORTION_CORRECT) {
            return null;
        }
        return new NamingResult(mostCommonDomain.getKey(), confidence);
    }

}
