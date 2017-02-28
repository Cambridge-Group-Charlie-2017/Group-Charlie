package uk.ac.cam.cl.charlie.clustering;

import uk.ac.cam.cl.charlie.clustering.clusterNaming.ClusterNamer;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by Ben on 01/02/2017.
 */
public abstract class Clusterer {

    private ClusterGroup clusters;
    //private Mailbox mailbox;

    private static VectorisingStrategy vectoriser = TfidfVectoriser.getVectoriser();
    public static VectorisingStrategy getVectoriser() {return vectoriser;}

    //alternatively, could convert to another form before returning.
    public ClusterGroup getClusters() {return clusters;}
    protected void setClusters(ClusterGroup clusters) {this.clusters = clusters;}

    //for inserting a list of messages into their appropriate clusters, and updating the server.
    public Hashtable<Message, String> classifyNewEmails(ArrayList<Message> messages) throws IncompatibleDimensionalityException {
        //For each new message, insert it into the ClusterGroup. This adds it into the cluster that bears the
        //closest match.
        getVectoriser().train(messages);
        Hashtable<Message, String> assignments = new Hashtable<>();

        for (int i = 0; i < messages.size(); i++) {
            int clusterNum = clusters.insert(new ClusterableMessage(messages.get(i)));
            String clusterName = clusters.get(clusterNum).getName();
            assignments.put(messages.get(i), clusterName);
        }
        return assignments;
    }

    //Produces clusters of messages. evalClusters() will actually update the IMAP server.
    protected abstract ClusterGroup run(ClusterableObjectGroup objects) throws Exception;

    //Should probably convert to run on wrapper types.
    //Can easily provide functions for conversion.
    protected void evalClusters(ArrayList<Message> messages) {
        //main method for evaluating clusters.
        //precondition: all Messages in 'message' are clear for clustering i.e. are not in protected folders.
        //call training methods in Vectoriser. If Vectorising model doesn't require training, these will be blank anyway.
        //postcondition: 'clusters' contains the new clustering, and all emails are in their new clusters on the server.

        getVectoriser().train(messages);

        ArrayList<ClusterableMessage> clusterableMessages = new ArrayList<>();
        for (Message m : messages) {
            ClusterableMessage clusterableMessage = new ClusterableMessage(m);
            clusterableMessages.add(clusterableMessage);
        }

        //sets 'clusters' field to new clusters based on the 'messages' input.
        try {
           clusters = run(new ClusterableMessageGroup(clusterableMessages));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        //Could possibly move this into the constructor of Cluster.
        for (Cluster c : clusters)
           ClusterNamer.name(c);

    }
}
