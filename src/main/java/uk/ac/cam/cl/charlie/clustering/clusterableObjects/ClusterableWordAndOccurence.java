package uk.ac.cam.cl.charlie.clustering.clusterableObjects;

import java.util.NoSuchElementException;

import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.math.Vector;

/**
 * Created by M Boyce on 11/02/2017.
 */
public class ClusterableWordAndOccurence extends ClusterableObject<String> {
    private int occurences;
    private int position;

    public ClusterableWordAndOccurence(String word, int occurences, int position) {
        super(word, null);
        this.occurences = occurences;
        this.position = position;
    }

    public int getOccurences() {
        return occurences;
    }

    @Override
    public boolean equals(Object o) {
        ClusterableWordAndOccurence word2 = (ClusterableWordAndOccurence) o;
        return (object.equals(word2.object) && occurences == word2.getOccurences());
    }

    @Override
    public Vector getVector() {
        if (vec == null) {
            try {
                vec = Clusterer.getVectoriser().word2vec(object).get();
            } catch (NoSuchElementException e) {
                vec = Vector.zero(300);
            }
        }
        return vec;
    }

    public int getPosition() {
        return position;
    }
}
