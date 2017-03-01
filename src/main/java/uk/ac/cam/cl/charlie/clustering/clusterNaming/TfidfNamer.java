package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.vec.tfidf.BasicWordCounter;
import uk.ac.cam.cl.charlie.vec.tfidf.PersistentWordCounter;

public class TfidfNamer extends ClusterNamer {

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
        ArrayList<Message> messages = new ArrayList<>();
        for (ClusterableObject<Message> obj : cluster.getObjects())
            messages.add(obj.getObject());

        PersistentWordCounter documentFrequencies = PersistentWordCounter.getInstance();
        BasicWordCounter termFrequencies = new BasicWordCounter();

        for (Message msg : messages) {
            try {
                BasicWordCounter counter = BasicWordCounter.count(msg.getSubject());

                for (String w : counter.words()) {
                    termFrequencies.increment(w);
                }
            } catch (MessagingException e) {

            }
        }
        // now need to zip the two counters together and produce a sorted list
        List<WordTFIDFPair> results = new ArrayList<>();

        for (String w : termFrequencies.words()) {
            double totalDocs = documentFrequencies.frequency("");
            double totalDocsWith = documentFrequencies.frequency(w);
            double tfidf = termFrequencies.frequency(w) * Math.log(totalDocs / totalDocsWith);
            results.add(new WordTFIDFPair(w, tfidf));
        }
        Collections.sort(results);
        if (results.size() == 0) {
            return null;
        } else if (results.size() < 3) {
            return new NamingResult(results.get(results.size() - 1).word, 1);
        } else {
            int i = results.size() - 1;
            String res = results.get(i).word + " " + results.get(i - 1).word + " " + results.get(i - 2).word;
            return new NamingResult(res, 1);
        }
    }

}
