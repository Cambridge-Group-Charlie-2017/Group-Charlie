package uk.ac.cam.cl.charlie.vec.tfidf.kvstore;

import uk.ac.cam.cl.charlie.math.Vector;

/**
 * Created by shyam on 15/02/2017.
 */
public class WordVectorPair {
    public String word;
    public Vector v;
    public WordVectorPair(String w, Vector v) {
        this.word = w;
        this.v = v;
    }
}
