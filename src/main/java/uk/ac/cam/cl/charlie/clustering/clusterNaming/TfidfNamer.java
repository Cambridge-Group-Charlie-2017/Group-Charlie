package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.commons.lang.WordUtils;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.vec.tfidf.PersistentWordCounter;

public class TfidfNamer extends ClusterNamer {

    private static final double MIN_PROPORTION_CORRECT = 0.2;

    private static class WordTFIDFPair implements Comparable<WordTFIDFPair> {
        public String word;
        public double tfidfval;

        public WordTFIDFPair(String w, double tfidf) {
            word = w;
            tfidfval = tfidf;
        }

        @Override
        public int compareTo(WordTFIDFPair o) {
            if (this.tfidfval == o.tfidfval) {
                return 0;
            } else {
                return this.tfidfval < o.tfidfval ? -1 : 1;
            }
        }
    }

    @Override
    public NamingResult name(Cluster<Message> cluster) {
        List<ClusterableObject<Message>> messages = cluster.getObjects();

        PositionedWordCounter counter = new PositionedWordCounter();

        for (ClusterableObject<Message> cm : messages) {
            Message m = cm.getObject();

            try {
                counter.count(m.getSubject());
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        PersistentWordCounter documentFrequencies = PersistentWordCounter.getInstance();

        // now need to zip the two counters together and produce a sorted list
        List<WordTFIDFPair> results = new ArrayList<>();

        for (String w : counter.words()) {
            double totalDocs = documentFrequencies.frequency("");
            double totalDocsWith = documentFrequencies.frequency(w);
            double tfidf = counter.frequency(w) * Math.log(totalDocs / (totalDocsWith + 1));
            results.add(new WordTFIDFPair(w, tfidf));
        }

        int cutOff = (int) (messages.size() * MIN_PROPORTION_CORRECT);

        List<String> wordsToUse = results.stream()
                // Remove stop words
                .filter(p -> !stopWords.contains(p.word))
                // Sort according to TF-IDF
                .sorted((p1, p2) -> Double.compare(p2.tfidfval, p1.tfidfval))
                // Take only top 5
                .limit(5)
                // Convert back to word
                .map(pair -> pair.word)
                // Cut-off
                .filter(w -> counter.frequency(w) > cutOff)
                // Sort in relative position order
                .sorted((w1, w2) -> counter.position(w1) - counter.position(w2))
                // Convert back to list
                .collect(Collectors.toList());

        // Generate cluster name
        String clusterName = String.join(" ", wordsToUse);

        if (!clusterName.isEmpty())
            return new NamingResult("[TFIDF]" + WordUtils.capitalize(clusterName), 1);

        return null;
    }

}
