package uk.ac.cam.cl.charlie.clustering.clusterableObjects;

import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.math.Vector;

/**
 * Created by M Boyce on 11/02/2017.
 */
public class ClusterableWord implements ClusterableObject {
    private String word;

    public ClusterableWord(String word) {
	this.word = word;
    }

    public String getWord() {
	return word;
    }

    @Override
    public Vector getVec() {
	return Clusterer.getVectoriser().word2vec(word).get();
    }

    @Override
    public boolean equals(Object word2) {
        return word.equals((String) word2);
    }
}
