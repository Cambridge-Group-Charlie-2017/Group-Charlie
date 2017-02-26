package uk.ac.cam.cl.charlie.clustering.clusterableObjects;

import java.util.NoSuchElementException;

import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.math.Vector;

/**
 * Created by M Boyce on 11/02/2017.
 */
public class ClusterableWordAndOccurence extends ClusterableObject {
    private String word;
    private int occurences;
    private int position;

    public ClusterableWordAndOccurence(String word,int occurences,int position){
        this.word = word;
        this.occurences = occurences;
        this.position = position;
    }

    public String getWord(){
        return word;
    }

    public int getOccurences(){return occurences;}

    @Override
    public boolean equals(Object o) {
        ClusterableWordAndOccurence word2 = (ClusterableWordAndOccurence) o;
        return (word.equals(word2.getWord()) && occurences == word2.getOccurences());
    }

    @Override
    public Vector getVec() {
        try {
            return Clusterer.getVectoriser().word2vec(word).get();
        } catch (NoSuchElementException e) {
            return null;
        }
    }
}
