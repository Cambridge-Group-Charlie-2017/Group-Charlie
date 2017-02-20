package uk.ac.cam.cl.charlie;

import uk.ac.cam.cl.charlie.vec.tfidf.kvstore.WordVecDB;

/**
 * Created by shyam on 20/02/2017.
 * Not a test class, really - just a script that loads the full db before any tests
 */
public class PopulateDB {
    public static void main (String[] args) {
        System.out.println("Loading vectors");
        WordVecDB.populateFromTextFile("src/main/resources/word2vec/wordvectors.txt");
        System.out.println("Finishing loading");
        System.exit(0); // gradle isn't too keen to just exit :/
    }
}
