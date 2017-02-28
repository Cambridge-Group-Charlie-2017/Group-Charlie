package uk.ac.cam.cl.charlie.vec.tfidf;

import java.util.Set;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializers;

/**
 * Class for accessing word counter information stored in the database.
 *
 * @author Gary Guo
 */
public class PersistentWordCounter extends WordCounter {

    private static PersistentWordCounter instance = null;
    protected Database database;
    protected PersistentMap<String, Integer> map;

    private PersistentWordCounter() {
        database = Database.getInstance();
        map = database.getMap("word_frequencies", Serializers.STRING, Serializers.INTEGER);
    }

    public static PersistentWordCounter getInstance() {
        if (instance == null) {
            instance = new PersistentWordCounter();
        }
        return instance;
    }

    @Override
    public void increment(String word, int n) {
	map.put(word, frequency(word) + n);
    }

    @Override
    public int frequency(String word) {
        Integer freq = map.get(word);
        if (freq == null)
            return 0;
        return freq;
    }

    @Override
    public Set<String> words() {
	    throw new UnsupportedOperationException("Querying all words from a persisted word counter is too expensive");
    }

}
