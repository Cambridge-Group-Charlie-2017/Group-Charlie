package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;

import org.apache.commons.lang.WordUtils;

import uk.ac.cam.cl.charlie.clustering.EMClusterer;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableWordAndOccurence;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;

/**
 * @author M Boyce
 */
public class ClusterNamer {
    private static HashSet<String> stopWords = new HashSet<>(StopWords.getStopWords());

    private static double MIN_PROPORTION_CORRECT = 0.8;

    /**
     * Given an cluster of emails generates and sets the name for the cluster
     * based on the emails in that clusters contents and sets the given clusters
     * name to that
     *
     * @param cluster
     */
    public static void subjectNaming(Cluster<Message> cluster) throws ClusterNamingException {
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
            cluster.setName(WordUtils.capitalize(clusterName));
        else {
            cluster.setName("Error Naming Cluster");
            throw new ClusterNamingException("Basic naming failed");
        }
    }

    /**
     * Given a cluster of emails generates and sets the name for the cluster
     * based on the sender of the emails in that cluster
     *
     * @param cluster
     */
    public static void senderNaming(Cluster<Message> cluster) throws ClusterNamingException {
        ArrayList<ClusterableObject<Message>> messages = cluster.getObjects();

        // Map storing number of occurrences of each domain name in the sender
        // address of the messages in the cluster
        TreeMap<String, Integer> domains = new TreeMap<>();

        for (int i = 0; i < messages.size(); i++) {
            try {
                Address[] from = messages.get(i).getObject().getFrom();
                String[] address = from[0].toString().split("@");

                // Remove the .com /.net ect
                String[] tempDomain = address[1].split("\\.");
                String domain = "";

                for (int j = 0; j < tempDomain.length - 1; j++) {
                    domain += tempDomain[j];
                }

                // Increment the count of relevant domain
                int count = 0;
                if (domains.containsKey(domain)) {
                    count = domains.get(domain);
                }
                domains.put(domain, count + 1);

            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        // Test to see if is a good name for cluster by seeing what proportion
        // of the Cluster this name holds for
        Map.Entry<String, Integer> mostCommonDomain = domains.lastEntry();
        if (mostCommonDomain.getValue() / messages.size() > MIN_PROPORTION_CORRECT) {
            cluster.setName(mostCommonDomain.getKey());
        } else {
            throw new ClusterNamingException("Cannot use sender as folder name");
        }
    }

    public static void word2VecNaming(Cluster<Message> cluster) throws Exception {
        ArrayList<Message> messages = new ArrayList<>();
        TreeMap<String, Integer> wordFrequencySubject = new TreeMap<>(Collections.reverseOrder());
        HashMap<String, Integer> wordTotalPositionMap = new HashMap<>();

        for (ClusterableObject<Message> obj : cluster.getObjects())
            messages.add(obj.getObject());

        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);

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

            } catch (MessagingException e) {
                System.out.println(e.getMessage());

            }
        }

        // Map to array list
        ArrayList<ClusterableWordAndOccurence> words = new ArrayList<>();
        Iterator<Map.Entry<String, Integer>> iterator = wordFrequencySubject.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();// TODO: Position
            // Calculate Relative positions
            int averagePosition = wordTotalPositionMap.get(entry.getKey()) / wordFrequencySubject.get(entry.getKey());
            ClusterableWordAndOccurence w = new ClusterableWordAndOccurence(entry.getKey(), entry.getValue(),
                    averagePosition);
            words.add(w);
        }

        EMClusterer<String> clusterer = new EMClusterer<>(words);
        ClusterGroup<String> clusters = clusterer.getClusters();
        String folderName = "";
        int cuttOff = (int) (messages.size() * MIN_PROPORTION_CORRECT);

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
        cluster.setName(folderName);
    }

    /**
     * Generic naming of the cluster that tries simple methods first aand if
     * they are not good enough try more complicated methods
     *
     * @param cluster
     */
    public static String name(Cluster<Message> cluster) {
        try {
            subjectNaming(cluster);
        } catch (ClusterNamingException e) {
            // subjectNaming naming method is not good enough
            try {
                senderNaming(cluster);
            } catch (ClusterNamingException e1) {
                // sender method is not good enough
                try {
                    word2VecNaming(cluster);
                } catch (Exception e2) {
                    try {
                        String subject = cluster.getObjects().get(0).getObject().getSubject();
                        if (subject.equals(""))
                            cluster.setName("Failed to name: " + Math.random());
                        else
                            cluster.setName(subject);
                    } catch (MessagingException e3) {
                        cluster.setName("Failed to Name" + Math.random());
                    }
                }

            }
        }
        return cluster.getName();
    }
}
