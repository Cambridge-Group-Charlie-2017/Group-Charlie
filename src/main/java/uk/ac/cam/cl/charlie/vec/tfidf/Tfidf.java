package uk.ac.cam.cl.charlie.vec.tfidf;

/**
 * Created by shyam on 05/02/2017.
 */
public final class Tfidf {
    // Presumably this will have to be stored in the database (which I will leave for later).
    // Use singleton pattern to stop subclassing

    private static Tfidf instance = null;

    private Tfidf() {
        // todo
    }

    public static Tfidf getInstance() {
        if (instance == null) {
            instance = new Tfidf();
        }
        return instance;
    }


    public int totalNumberDocuments() {
        // todo
        return 0;
    }
    public int numberOfDocsWithWith(String word) {
        // todo
        return 0;
    }
}
