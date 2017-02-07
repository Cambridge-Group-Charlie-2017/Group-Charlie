package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by Ben on 01/02/2017.
 */
public abstract class Clusterer {
    //Possible implentations:
    //KMeans, XMeans, EM, etc.
    private ArrayList<Cluster> clusters = new ArrayList<Cluster>();

    //Mailbox is private because only this class should update it, no inheriting class.
    //private Mailbox mailbox;

    //the vector array is only temporary until the Vectoriser is implemented.
    ArrayList<Vector<Double>> vecsForTesting = new ArrayList<Vector<Double>>();

    public ArrayList<Cluster> getClusters() {return clusters;}

    //for inserting a list of messages into their appropriate clusters, and updating the server.
    void classifyNewEmails(ArrayList<Message> messages) throws VectorElementMismatchException {
        //gets temp test vectors. Update once Vectoriser is implemented to getVecs(messages) or something.
        ArrayList<Vector<Double>> vecs = vecsForTesting;

        //For classification, find best clustering for each email using matchStrength().
        //TODO: Update mailbox accordingly.
        double bestMatch = Integer.MAX_VALUE;
        int bestCluster = 0;
        for (int i = 0; i < messages.size(); i++) {
            for (int j = 0; j < clusters.size(); j++) {
                double currMatch = clusters.get(j).matchStrength(vecs.get(i));
                if (currMatch < bestMatch) {
                    bestMatch = currMatch;
                    bestCluster = j;
                }
            }
            clusters.get(bestCluster).addMessage(messages.get(i));
        }
    }

    //Produces clusters of messages. evalClusters() will actually update the IMAP server.
    protected abstract ArrayList<Cluster> run(ArrayList<Message> vecs) throws Exception;


    public void evalClusters(ArrayList<Message> messages) {
        //main method for evaluating clusters.
        //precondition: all Messages in 'message' are clear for clustering i.e. are not in protected folders.
        //call training methods in Vectoriser. If Vectorising model doesn't require training, these will be blank anyway.
        //postcondition: 'clusters' contains the new clustering, and all emails are in their new clusters on the server.


        //sets 'clusters' field to new clusters based on the 'messages' input.
        try {
            clusters = run(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //TODO: uncomment naming function when actual emails are used.
        for (Cluster c : clusters)
           //ClusterNamer.name(c);

        //TODO: update server with new clusters.
        return;
    }
}
