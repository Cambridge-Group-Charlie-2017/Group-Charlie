package uk.ac.cam.cl.charlie.clustering;

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
	return GenericClusterer.getVectoriser().word2vec(word).get();
    }

    // TODO: write equals() function
}
