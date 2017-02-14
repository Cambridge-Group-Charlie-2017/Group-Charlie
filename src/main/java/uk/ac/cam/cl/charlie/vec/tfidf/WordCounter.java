package uk.ac.cam.cl.charlie.vec.tfidf;

import java.util.Set;

/**
 * Abstract class for word counters
 *
 * @author Gary Guo
 */
public abstract class WordCounter {
    /**
     * Increment word frequency in the counter by {@code count}.
     *
     * @param word
     *            the word of which the frequency will be incremented
     * @param count
     *            number to increment
     */
    public abstract void increment(String word, int count);

    /**
     * Increment word frequency in the counter by 1.
     *
     * @param word
     *            the word of which the frequency will be incremented
     */
    public void increment(String word) {
	increment(word, 1);
    }

    /**
     * Get the frequency of a word in the counter.
     *
     * @param word
     *            the word of which the frequency will be retrieved
     * @return frequency related to the word in the counter
     */
    public abstract int frequency(String word);

    /**
     * Get all words appeared in this counter.
     *
     * @return a {@code Set} of all words appeared at least once
     */
    public abstract Set<String> words();
}
