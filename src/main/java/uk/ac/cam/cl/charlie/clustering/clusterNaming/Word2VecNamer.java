package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.ArrayList;
import java.util.HashMap;

import javax.mail.Message;
import javax.mail.MessagingException;

import uk.ac.cam.cl.charlie.clustering.EMClusterer;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableWordAndOccurence;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;
import uk.ac.cam.cl.charlie.vec.tfidf.BasicWordCounter;

public class Word2VecNamer extends ClusterNamer {

    @Override
    public NamingResult name(Cluster<Message> cluster) {
        ArrayList<Message> messages = new ArrayList<>();
        BasicWordCounter wordFrequencySubject = new BasicWordCounter();
        HashMap<String, Integer> wordTotalPositionMap = new HashMap<>();

        for (ClusterableObject<Message> obj : cluster.getObjects())
            messages.add(obj.getObject());

        for (Message m : messages) {
            try {
                String[] subjectWords = m.getSubject().toLowerCase().split("[^A-Za-z0-9']");

                for (int i = 0; i < subjectWords.length; i++) {
                    if (subjectWords[i].isEmpty())
                        continue;
                    if (!stopWords.contains(subjectWords[i])) {
                        wordFrequencySubject.increment(subjectWords[i]);

                        int curPosTotal = 0;
                        if (wordTotalPositionMap.containsKey(subjectWords[i])) {
                            curPosTotal = wordTotalPositionMap.get(subjectWords[i]);
                        }
                        wordTotalPositionMap.put(subjectWords[i], curPosTotal + i);
                    }
                }

            } catch (MessagingException e) {
                System.out.println(e.getMessage());

            }
        }

        // Map to array list
        ArrayList<ClusterableWordAndOccurence> words = new ArrayList<>();
        for (String w : wordFrequencySubject.words()) {
            // Calculate Relative positions
            int freq = wordFrequencySubject.frequency(w);
            int averagePosition = wordTotalPositionMap.get(w) / freq;
            ClusterableWordAndOccurence cw = new ClusterableWordAndOccurence(w, freq, averagePosition);
            words.add(cw);
        }

        EMClusterer<String> clusterer = new EMClusterer<>(words);
        ClusterGroup<String> clusters = clusterer.getClusters();
        String folderName = "";
        // int cuttOff = (int) (messages.size() * MIN_PROPORTION_CORRECT);

        ArrayList<ClusterableWordAndOccurence> wordsToUse = new ArrayList<>();

        for (int i = 0; i < clusters.size(); i++) {
            Cluster<String> gc = clusters.get(i);
            ArrayList<ClusterableObject<String>> w = gc.getObjects();

            // Get most common word in a cluster
            int mostCommonOccurences = 0;
            ClusterableWordAndOccurence mostCommonWord = null;
            for (int j = 0; j < w.size(); j++) {
                ClusterableWordAndOccurence word = (ClusterableWordAndOccurence) w.get(j);
                if (word.getOccurences() > mostCommonOccurences) {
                    mostCommonOccurences = word.getOccurences();
                    mostCommonWord = word;
                }
            }
            wordsToUse.add(mostCommonWord);
        }

        // Sort words to use based on Average positions
        for (int i = 0; i < wordsToUse.size(); i++) {
            ClusterableWordAndOccurence tempWord = wordsToUse.get(i);
            for (int j = i - 1; j >= 0 && wordsToUse.get(i).getPosition() < wordsToUse.get(j).getPosition(); j--) {
                wordsToUse.set(j + 1, wordsToUse.get(j));
                wordsToUse.set(j, tempWord);
            }
        }

        // Create clusterName
        for (ClusterableWordAndOccurence aWordsToUse : wordsToUse) {
            folderName += aWordsToUse.getObject() + " ";
        }

        // Set folderName
        return new NamingResult(folderName, 0.6);
    }

}
