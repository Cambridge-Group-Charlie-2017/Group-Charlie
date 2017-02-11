package uk.ac.cam.cl.charlie.clustering;

import uk.ac.cam.cl.charlie.vec.TextVector;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;

import java.util.Vector;

/**
 * Created by M Boyce on 11/02/2017.
 */
public class ClusterableWord implements ClusterableObject {
    private String word;

    public ClusterableWord(String word){
        this.word = word;
    }

    public String getWord(){
        return word;
    }

    @Override
    public TextVector getVec() {
        TfidfVectoriser vectoriser = new TfidfVectoriser();
        vectoriser.word2vec(word);
        //TODO: Implement correct vectorising
        return GenericDummyVectoriser.vectorise(this);
    }
}
