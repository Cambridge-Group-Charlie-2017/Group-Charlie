package uk.ac.cam.cl.charlie.clustering;

/**
 * @author M Boyce
 */
public class WordAndOccurences {
    private String word;
    private int occurences;

    public WordAndOccurences(String word,int occurences){
        this.word = word;
        this.occurences = occurences;
    }

    public int getOccurences() {
        return occurences;
    }
}
