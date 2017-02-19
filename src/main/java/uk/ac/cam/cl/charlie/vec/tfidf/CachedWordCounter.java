package uk.ac.cam.cl.charlie.vec.tfidf;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Speeding up counter access by caching in memory.
 *
 * @author Gary Guo
 * @author Shyam
 */
public class CachedWordCounter extends BasicWordCounter {

    protected WordCounter upstream;
    protected Cache<String, Integer> cache;

    public CachedWordCounter(WordCounter counter) {
        upstream = counter;
        cache = CacheBuilder.newBuilder().maximumSize(20000).build(); // change size as needed
    }

    @Override
    public int frequency(String word) {
        try {
            Integer value = cache.get(word, new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    return upstream.frequency(word);
                }
            });

            return value;
        } catch (ExecutionException e) {
            throw new Error(e);
        }
    }

    @Override
    public void increment (String word, int n) {
        int currentVal = frequency(word);
        cache.put(word, currentVal + n);
        upstream.increment(word, n); // perhaps worth looking at multithreading?
    }


    @Override
    public Set<String> words() {
        throw new UnsupportedOperationException("Querying all words from a persisted word counter is too expensive");
    }

}
