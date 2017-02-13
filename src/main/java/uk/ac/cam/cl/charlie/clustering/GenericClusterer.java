package uk.ac.cam.cl.charlie.clustering;

import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;

import java.util.ArrayList;

/**
 * Created by Ben on 01/02/2017.
 */
public abstract class GenericClusterer {

    private GenericClusterGroup clusters;
    //private Mailbox mailbox;

    private static VectorisingStrategy vectoriser = new TfidfVectoriser();
    public static VectorisingStrategy getVectoriser() {return vectoriser;}

    public GenericClusterGroup getClusters() {return clusters;}

    //for inserting a list of messages into their appropriate clusters, and updating the server.
    public abstract void classifyNewEmails(ArrayList<ClusterableObject> objects) throws IncompatibleDimensionalityException;

    //Produces clusters of messages. evalClusters() will actually update the IMAP server.
    protected abstract GenericClusterGroup run(ArrayList<ClusterableObject> vecs) throws Exception;


    public void evalClusters(ArrayList<ClusterableObject> clusterableObjects) {
        //main method for evaluating clusters.
        //precondition: all Messages in 'message' are clear for clustering i.e. are not in protected folders.
        //call training methods in Vectoriser. If Vectorising model doesn't require training, these will be blank anyway.
        //postcondition: 'clusters' contains the new clustering, and all emails are in their new clusters on the server.


        //sets 'clusters' field to new clusters based on the 'messages' input.
        try {
            clusters = run(clusterableObjects);
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
}
