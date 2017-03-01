package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.HashMap;
import java.util.Set;

import uk.ac.cam.cl.charlie.util.IntHolder;

public class PositionedWordCounter {

    HashMap<String, IntHolder> frequency = new HashMap<>();
    HashMap<String, IntHolder> positions = new HashMap<>();

    public void addWord(String word, int position) {
        IntHolder h = frequency.get(word);
        IntHolder p = positions.get(word);
        if (h == null) {
            h = new IntHolder();
            p = new IntHolder();
            frequency.put(word, h);
            positions.put(word, p);
        }
        h.value++;
        p.value += position;
    }

    public int frequency(String word) {
        IntHolder h = frequency.get(word);
        if (h == null) {
            return 0;
        }
        return h.value;
    }

    public int position(String word) {
        IntHolder p = positions.get(word);
        if (p == null) {
            return 0;
        }
        return p.value / frequency(word);
    }

    /**
     * Get all words appeared in this counter.
     *
     * @return a {@code Set} of all words appeared at least once
     */
    public Set<String> words() {
        return frequency.keySet();
    }

    /**
     * Count words in the given text and generate statistics.
     *
     * @param text
     *            text to count on
     * @return a {@code WordCounter} instance containing the statistics
     */
    public void count(String text) {
        String[] words = text.split("[^A-Za-z0-9']");
        int pos = 0;
        for (String word : words) {
            if (word.isEmpty())
                continue;
            addWord(word.toLowerCase(), pos++);
        }
    }

}
