package uk.ac.cam.cl.charlie.clustering;

import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;

import javax.mail.Message;
import java.util.ArrayList;

/**
 * Created by Ben on 01/02/2017.
 */
public abstract class GenericClusterer {

    private GenericClusterGroup clusters;
    //private Mailbox mailbox;

    private static VectorisingStrategy vectoriser = TfidfVectoriser.getVectoriser();
    public static VectorisingStrategy getVectoriser() {return vectoriser;}

    public GenericClusterGroup getClusters() {return clusters;}

    //for inserting a list of messages into their appropriate clusters, and updating the server.
    public void classifyNewEmails(ArrayList<Message> messages) throws IncompatibleDimensionalityException {
        GenericClusterGroup clusters = getClusters();

        //For classification, find clustering with highest probability for each email using matchStrength().
        //TODO: Update mailbox accordingly. Could be a method in Clusterer itself.

        //TODO: update to run using clusterGroup.insert()


        //For each new message,
        for (int i = 0; i < messages.size(); i++) {
            double bestMatch = Integer.MAX_VALUE;
            int bestCluster = 0;
            //Find the index of the best cluster,
            for (int j = 0; j < clusters.size(); j++) {
                double currMatch = clusters.get(j).matchStrength(new ClusterableMessage(messages.get(i)));
                if (currMatch > bestMatch) {
                    bestMatch = currMatch;
                    bestCluster = j;
                }
            }
            //and insert the message into that cluster.
            clusters.get(bestCluster).addMessage(new ClusterableMessage(messages.get(i)));
        }
    }

    //Produces clusters of messages. evalClusters() will actually update the IMAP server.
    public abstract GenericClusterGroup run(ClusterableObjectGroup objects) throws Exception;


    //Should probably convert to run on wrapper types.
    //Can easily provide functions for conversion.
    public void evalClusters(ArrayList<Message> messages) {
        //main method for evaluating clusters.
        //precondition: all Messages in 'message' are clear for clustering i.e. are not in protected folders.
        //call training methods in Vectoriser. If Vectorising model doesn't require training, these will be blank anyway.
        //postcondition: 'clusters' contains the new clustering, and all emails are in their new clusters on the server.

        ArrayList<ClusterableMessage> clusterableMessages = new ArrayList<>();
        for (Message m : messages) {
            clusterableMessages.add(new ClusterableMessage(m));
        }

        //sets 'clusters' field to new clusters based on the 'messages' input.
        try {
            clusters = run(new ClusterableMessageGroup(clusterableMessages));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //TODO: uncomment naming function when actual emails are used.
        //Could possibly move this into the constructor of Cluster.
        for (GenericCluster c : clusters)
           //ClusterNamer.name(c);

        //TODO: update server with new clusters.
        return;
    }

    public void clusterWords(ArrayList<ClusterableObject> words) {

    }
}
