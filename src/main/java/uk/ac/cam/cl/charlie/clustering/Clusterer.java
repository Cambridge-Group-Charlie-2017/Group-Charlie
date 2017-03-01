package uk.ac.cam.cl.charlie.clustering;

import java.util.List;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;

/**
 * Created by Ben on 01/02/2017.
 */
public abstract class Clusterer<T> {

    private ClusterGroup<T> clusters;

    private static VectorisingStrategy vectoriser = TfidfVectoriser.getVectoriser();

    public static VectorisingStrategy getVectoriser() {
        return vectoriser;
    }

    // alternatively, could convert to another form before returning.
    public ClusterGroup<T> getClusters() {
        return clusters;
    }

    protected void setClusters(ClusterGroup<T> clusters) {
        this.clusters = clusters;
    }

    // Produces clusters of messages.
    protected abstract ClusterGroup<T> run(List<? extends ClusterableObject<T>> objects) throws Exception;

    // Should probably convert to run on wrapper types.
    // Can easily provide functions for conversion.
    protected void evalClusters(List<? extends ClusterableObject<T>> objects) {
        // main method for evaluating clusters.
        // precondition: all Messages in 'message' are clear for clustering i.e.
        // are not in protected folders.
        // call training methods in Vectoriser. If Vectorising model doesn't
        // require training, these will be blank anyway.
        // postcondition: 'clusters' contains the new clustering, and all emails
        // are in their new clusters on the server.

        // sets 'clusters' field to new clusters based on the 'messages' input.
        try {
            clusters = run(objects);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

    }

}
