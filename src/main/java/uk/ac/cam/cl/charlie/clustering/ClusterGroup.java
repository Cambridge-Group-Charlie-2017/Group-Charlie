package uk.ac.cam.cl.charlie.clustering;

/**
 * Created by Ben on 07/02/2017.
 */
public abstract class ClusterGroup {
    /*
     * Purpose of this class:
     * Act as a wrapper for the Cluster objects. Instead of passing around ArrayList<Cluster>, pass ClusterGroup.
     * It also provides other methods, such as:
     * get(i); to get cluster at index i
     * int insert(Message) to insert a message into the best cluster. Returns index of cluster it inserted into.
     * saveState(String filename) to save the current state of the clusters in a file
     * constructor ClusterGroup(String filename) to load from a state
     * constructor ClusterGroup() - default constructor, no clusters
     * add(Cluster) to add a cluster
     */
}
