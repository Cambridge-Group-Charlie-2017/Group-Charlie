package uk.ac.cam.cl.charlie.vec.tfidf;

import java.util.HashMap;
import java.util.Set;

/**
 * Speeding up counter access by caching in memory.
 *
 * @author Gary Guo
 */
public class CachedWordCounter extends BasicWordCounter {

    protected WordCounter upstream;
    protected HashMap<String, Integer> cache = new HashMap<>();

    public CachedWordCounter(WordCounter counter) {
        upstream = counter;
    }

    @Override
    public int frequency(String word) {
        Integer value = cache.get(word);
        if (value == null) {
            value = upstream.frequency(word);
            cache.put(word, value);
        }
        return value + super.frequency(word);
    }

    /**
     * Flush any local changes to the upstream and clear the cache
     */
    public void synchronize() {
        for (String w : super.words()) {
            upstream.increment(w, super.frequency(w));
        }
        this.map.clear();
        this.cache.clear();
    }

    @Override
    public Set<String> words() {
        throw new UnsupportedOperationException("Querying all words from a persisted word counter is too expensive");
    }

    @Override
    public void finalize() {
        if (this.map.size() != 0) {
            System.err.println("CachedWordCounter.synchronize should be called manually for synchronization");
            synchronize();
        }
    }

}
