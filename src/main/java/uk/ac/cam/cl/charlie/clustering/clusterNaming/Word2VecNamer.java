package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;
import javax.mail.MessagingException;

import uk.ac.cam.cl.charlie.clustering.EMClusterer;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableWordAndOccurence;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;

public class Word2VecNamer extends ClusterNamer {

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

        // Calculate average position
        ArrayList<ClusterableWordAndOccurence> words = new ArrayList<>();
        for (String w : counter.words()) {
            if (stopWords.contains(w))
                continue;
            int freq = counter.frequency(w);
            int averagePosition = counter.position(w);
            ClusterableWordAndOccurence cw = new ClusterableWordAndOccurence(w, freq, averagePosition);
            words.add(cw);
        }

        try {
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


            //Calculate confindence
            double confidence = 0;
            for (ClusterableWordAndOccurence word : wordsToUse) {
                confidence += word.getOccurences();
            }
            confidence /= counter.getWordCount();
            confidence /= wordsToUse.size();

            // Set folderName
            return new NamingResult(folderName, confidence);
        }catch(Exception exp){
            //Sometimes clusterer fails to vectorise any words so clusterer fails
            return null;
        }
    }

}
