package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.commons.lang.WordUtils;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;

/**
 * Given an cluster of emails generates and sets the name for the cluster based
 * on the emails in that clusters contents and sets the given clusters name to
 * that
 *
 * @author M Boyce
 */
public class SubjectNamer extends ClusterNamer {

    private static double MIN_PROPORTION_CORRECT = 0.8;

    @Override
    public NamingResult name(Cluster<Message> cluster) {
        ArrayList<ClusterableObject<Message>> messages = cluster.getObjects();

        HashMap<String, Integer> wordFrequencySubject = new HashMap<>();
        HashMap<String, Integer> wordTotalPositionMap = new HashMap<>();
        // NOTE: wordAveragePositionMap contains sum of position in subject line
        // to start with later
        // divide by its frequency to get aveage position for word ordering

        // TODO: Possibly include body of email in naming
        // TreeMap<String,Integer> wordFrequencyBody = new
        // TreeMap<String,Integer>();

        // Loop through all emails in a cluster and find word frequencies
        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i).getObject();

            try {
                String[] subjectWords = m.getSubject().toLowerCase().split(" ");
                for (int j = 0; j < subjectWords.length; j++) {
                    if (!stopWords.contains(subjectWords[j])) {
                        int count = 0;
                        int curPosTotal = 0;
                        if (wordFrequencySubject.containsKey(subjectWords[j])) {
                            count = wordFrequencySubject.get(subjectWords[j]);
                            curPosTotal = wordTotalPositionMap.get(subjectWords[j]);
                        }
                        wordFrequencySubject.put(subjectWords[j], count + 1);
                        wordTotalPositionMap.put(subjectWords[j], curPosTotal + j);
                    }
                }

                // Return most common word

            } catch (MessagingException e) {
                System.out.println(e.getMessage());

            }

        }

        // Generate cluster name
        String clusterName = "";
        ArrayList<String> wordsToUse = new ArrayList<>();

        Iterator<Map.Entry<String, Integer>> iterator = wordFrequencySubject.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).iterator();

        // Generate cut off for number of occurrences
        int cutOff = (int) (messages.size() * MIN_PROPORTION_CORRECT);

        Map.Entry<String, Integer> curEntry = iterator.next();
        int count = 0;
        while (iterator.hasNext() && curEntry != null && curEntry.getValue() > cutOff && count < 5) {
            wordsToUse.add(curEntry.getKey());
            curEntry = iterator.next();
            count++;
        }

        int[] averagePositions = new int[wordsToUse.size()];

        // Calculate Relative positions
        for (int i = 0; i < averagePositions.length; i++) {
            averagePositions[i] = wordTotalPositionMap.get(wordsToUse.get(i))
                    / wordFrequencySubject.get(wordsToUse.get(i));
        }

        // Sort words to use based on Average positions
        for (int i = 0; i < averagePositions.length; i++) {
            int tempPos = averagePositions[i];
            String tempString = wordsToUse.get(i);
            for (int j = i - 1; j >= 0 && tempPos < averagePositions[j]; j--) {
                averagePositions[j + 1] = averagePositions[j];
                averagePositions[j] = tempPos;
                wordsToUse.set(j + 1, wordsToUse.get(j));
                wordsToUse.set(j, tempString);
            }
        }

        // Create clusterName
        for (String aWordsToUse : wordsToUse) {
            clusterName += aWordsToUse + " ";
        }

        // Set cluster name
        if (!(clusterName.toLowerCase().equals("") || clusterName.toLowerCase().equals("re: ")
                || clusterName.toLowerCase().equals("fwd: ")))
            return new NamingResult(WordUtils.capitalize(clusterName), 1);
        else {
            return null;
        }
    }

}
