package uk.ac.cam.cl.charlie.clustering.clusterableObjects;

import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.math.Vector;

/**
 * @author M Boyce
 * @author Gary Guo
 */
public class ClusterableWord extends ClusterableObject<String> {

    public ClusterableWord(String word) {
        super(word, null);
    }

    @Override
    public Vector getVector() {
        if (vec == null) {
            vec = Clusterer.getVectoriser().word2vec(object).get();
        }
        return vec;
    }

}
