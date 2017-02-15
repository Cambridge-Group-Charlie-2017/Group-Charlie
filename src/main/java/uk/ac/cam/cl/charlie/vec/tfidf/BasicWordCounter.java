package uk.ac.cam.cl.charlie.vec.tfidf;

import java.util.HashMap;
import java.util.Set;

/**
 * Utility for generating and gathering word frequency statistics.
 *
 * @author Gary Guo
 */
public class BasicWordCounter extends WordCounter {

    protected static class IntHolder {
        public int value;
    }

    protected HashMap<String, IntHolder> map = new HashMap<>();

    /**
     * Increment word frequency in the counter.
     *
     * @param word the word of which the frequency will be incremented
     */
    @Override
    public void increment(String word, int n) {
        IntHolder h = map.get(word);
        if (h == null) {
            h = new IntHolder();
            map.put(word, h);
        }
        h.value += n;
    }

    /**
     * Get the frequency of a word in the counter.
     *
     * @param word the word of which the frequency will be retrieved
     * @return frequency related to the word in the counter
     */
    @Override
    public int frequency(String word) {
        IntHolder h = map.get(word);
        if (h == null) {
            return 0;
        }
        return h.value;
    }

    /**
     * Get all words appeared in this counter.
     *
     * @return a {@code Set} of all words appeared at least once
     */
    @Override
    public Set<String> words() {
        return map.keySet();
    }

    /**
     * Count words in the given text and generate statistics.
     *
     * @param text text to count on
     * @return a {@code WordCounter} instance containing the statistics
     */
    public static BasicWordCounter count(String text) {
        String[] words = text.split("[^A-Za-z0-9']");
        BasicWordCounter counter = new BasicWordCounter();
        for (String word : words) {
            if (word.isEmpty())
                continue;
            counter.increment(word.toLowerCase());
        }
        return counter;

    }
}
