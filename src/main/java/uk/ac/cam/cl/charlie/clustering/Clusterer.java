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

    private static VectorisingStrategy vectoriser = TfidfVectoriser.getVectoriser();

    public static VectorisingStrategy getVectoriser() {
        return vectoriser;
    }

    public abstract ClusterGroup<T> cluster(List<? extends ClusterableObject<T>> objects);

}
