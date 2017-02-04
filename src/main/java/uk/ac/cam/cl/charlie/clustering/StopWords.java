package uk.ac.cam.cl.charlie.clustering;

import java.util.HashSet;

/**
 * Created by M Boyce on 04/02/2017.
 */
public class StopWords {
    private static StopWords ourInstance = new StopWords();

    private static HashSet<String> stopWords;
    private StopWords() {
        stopWords.add("the");
        stopWords.add("a");
        stopWords.add("and");
    }

    public static HashSet<String> getStopWords(){
        return stopWords;
    }
}
