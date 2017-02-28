package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.io.IOException;
import java.util.*;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;

import org.apache.commons.lang.WordUtils;
import uk.ac.cam.cl.charlie.clustering.ClusterableWordGroup;
import uk.ac.cam.cl.charlie.clustering.EMClusterer;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableWordAndOccurence;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;
import uk.ac.cam.cl.charlie.vec.tfidf.CachedWordCounter;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;
import uk.ac.cam.cl.charlie.mail.Messages;
import uk.ac.cam.cl.charlie.vec.tfidf.BasicWordCounter;

/**
 * @author M Boyce
 */
public class ClusterNamer {
    private static HashSet<String> stopWords = new HashSet<>(StopWords.getStopWords());

    // Controls the max occurrences of a word before it is considered a stop word
    private static final double MAX_OCCURRENCE = 0.2;
    // Controls the proportion of email that need a certain feature for it to be accepted
    private static double MIN_PROPORTION_CORRECT = 0.8;

    /**
     * Given an cluster of emails generates and sets the name for the cluster
     * based on the emails in that clusters contents and sets the given clusters
     * name to that
     * 
     * @param cluster
     */
    public static void subjectNaming(Cluster cluster) throws ClusterNamingException {
        ArrayList<ClusterableObject> messages = cluster.getContents();

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
            Message m = ((ClusterableMessage) messages.get(i)).getMessage();

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
    public static void senderNaming(Cluster cluster) throws ClusterNamingException {
	ArrayList<ClusterableObject> messages = cluster.getContents();

	// Map storing number of occurrences of each domain name in the sender
	// address of the messages in the cluster
	TreeMap<String, Integer> domains = new TreeMap<>();

	for (int i = 0; i < messages.size(); i++) {
	    try {
            Address[] from = ((ClusterableMessage) messages.get(i)).getMessage().getFrom();
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


    public static void word2VecNaming(Cluster cluster) throws Exception {
        TfidfVectoriser vectoriser = TfidfVectoriser.getVectoriser();
        CachedWordCounter persistentWordCounter = vectoriser.getWordCounter();
        double totalNumberOfDocs = vectoriser.getTotalNumberOfDocs();


        ArrayList<Message> messages = new ArrayList<>();
        TreeMap<String, Integer> wordFrequencySubject = new TreeMap<>(Collections.reverseOrder());
        HashMap<String, Integer> wordTotalPositionMap = new HashMap<>();

        for(ClusterableObject obj : cluster.getContents())
            messages.add(((ClusterableMessage)obj).getMessage());

        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);

            try {
            String[] subjectWords = m.getSubject().toLowerCase().split(" ");
            for (int j = 0; j < subjectWords.length; j++) {
                if ((persistentWordCounter.frequency(subjectWords[j]) / totalNumberOfDocs)< MAX_OCCURRENCE){//!stopWords.contains(subjectWords[j])) {
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
            Map.Entry<String, Integer> entry = iterator.next();//TODO: Position
            // Calculate Relative positions
            int averagePosition = wordTotalPositionMap.get(entry.getKey())
                        / wordFrequencySubject.get(entry.getKey());
            ClusterableWordAndOccurence w = new ClusterableWordAndOccurence(entry.getKey(), entry.getValue(),averagePosition);
            words.add(new ClusterableWordAndOccurence(entry.getKey(), entry.getValue(),averagePosition));
        }
        ClusterableWordGroup toCluster = new ClusterableWordGroup(words);

        EMClusterer clusterer = new EMClusterer(toCluster);
        ClusterGroup clusters = clusterer.getClusters();
        String folderName = "";
        int cuttOff = (int) (messages.size() * MIN_PROPORTION_CORRECT);

        ArrayList<ClusterableWordAndOccurence> wordsToUse = new ArrayList<>();

        for (int i = 0; i < clusters.size(); i++) {
            Cluster gc = clusters.get(i);
            ArrayList<ClusterableObject> w = gc.getContents();

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
            folderName += aWordsToUse.getWord() + " ";
        }


        // Set folderName
        cluster.setName(folderName);
    }

    /**
     * Tries to find the most common words in email that do not appear as often in
     * other clusters
     * @param clusters
     */
    public static void clusterGroupNaming(ClusterGroup clusters) {
        ArrayList<ArrayList<ClusterableWordAndOccurence>> clusterWordCounts = new ArrayList<>();
        //Count number of occurrences of words in each cluster's message's subject line
        for(Cluster c : clusters){
            clusterWordCounts.add(countWords(c,20));
        }
        //Check to see if is any other clusters name
        for(int i = 0; i < clusters.size(); i++){
            ArrayList<ClusterableWordAndOccurence> currentClusterWords = clusterWordCounts.get(i);
            ArrayList<ClusterableWordAndOccurence> wordsToUse = new ArrayList<>();
            boolean canUseWord = true;
            //Check against other clusters
            Iterator<ClusterableWordAndOccurence> currentClusterIterator = currentClusterWords.iterator();
            while(currentClusterIterator.hasNext()) {
                ClusterableWordAndOccurence currentWord = currentClusterIterator.next();
                for (int j = i + 1; j < clusters.size(); j++) {
                    ArrayList<ClusterableWordAndOccurence> checkingAgainstCluster = clusterWordCounts.get(j);
                    Iterator<ClusterableWordAndOccurence> checkingAgainstIterator = checkingAgainstCluster.iterator();
                    while(checkingAgainstIterator.hasNext()){
                        ClusterableWordAndOccurence checkingWord = checkingAgainstIterator.next();
                        if(currentWord.getWord() == checkingWord.getWord()){
                            //Remove Word From Both
                            checkingAgainstIterator.remove();
                            currentClusterIterator.remove();
                            canUseWord = false;
                        }
                    }
                }
                if(canUseWord){
                    //Add Word to cluster name
                    wordsToUse.add(currentWord);
                }
            }

            //Try Setting name
            String folderName = wordsToUseToString(wordsToUse);
            if(folderName.equals("")){
                name(clusters.get(i));
            }else{
                clusters.get(i).setName(folderName +"Temp: Cluster Group Namer");//TODO: REMOVE
            }
        }
    }

    /**
     * Generic naming of the cluster that tries simple methods first aand if
     * they are not good enough try more complicated methods
     * 
     * @param cluster
     */
    public static String name(Cluster cluster) {
//        try {
//            subjectNaming(cluster);
//        } catch (ClusterNamingException e) {
//            // subjectNaming naming method is not good enough
//            try {
//                senderNaming(cluster);
//            } catch (ClusterNamingException e1) {
//                // sender method is not good enough
//                try {
//                    word2VecNaming(cluster);
//                } catch (Exception e2) {
//                    try {
//                        String subject = ((ClusterableMessage) cluster.getContents().get(0)).getMessage().getSubject();
//                        if(subject.equals(""))
//                            cluster.setName("Failed to name: " + Math.random()) ;
//                        else
//                            cluster.setName(subject);
//                    } catch (MessagingException e3) {
//                        cluster.setName("Failed to Name" + Math.random());
//                    }
//                }
//
//            }
        return nameTFIDF(cluster);
    }

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
            }
            else {
                return this.tfidfval < o.tfidfval ? -1 : 1;
            }
        }
    }

    private static double calcTFIDF(int totalNumberDocs, int termFrequency, int numberDocsAppearedIn) {
        return (double) termFrequency * Math.log((double) totalNumberDocs / (double) numberDocsAppearedIn);
    }


    private static String nameTFIDF(Cluster cluster) {
        ArrayList<Message> messages = new ArrayList<>();
        for(ClusterableObject obj : cluster.getContents())
            messages.add(((ClusterableMessage)obj).getMessage());

        BasicWordCounter documentFrequency = new BasicWordCounter();
        BasicWordCounter termFrequencies = new BasicWordCounter();
        int numberOfMessages = messages.size();

        for (Message msg : messages) {
            Map<String,Integer> counts = new HashMap<>();
            try {
                String[] words = Messages.getBodyText(msg).split("[^A-Za-z0-9']");
                // count the words
                for (int i = 0; i < words.length; ++i) {
                    if (counts.containsKey(words[i])) {
                        counts.put(words[i], counts.get(words[i]) + 1); // increment by 1
                    }
                }
                // update term frequency with total, and document frequency by 1 if word was present
                for (String w : counts.keySet()) {
                    documentFrequency.increment(w, 1);
                    termFrequencies.increment(w, counts.get(w));
                }
            } catch (MessagingException | IOException e) {
                throw new Error(e);
            }
        }
        // now need to zip the two counters together and produce a sorted list
        List<WordTFIDFPair> results = new ArrayList<>();
        for (String w : documentFrequency.words()) {
            double tfidf = calcTFIDF(numberOfMessages, termFrequencies.frequency(w), documentFrequency.frequency(w));
            results.add(new WordTFIDFPair(w, tfidf));
        }
        Collections.sort(results);
        return results.get(results.size() - 1).word;
    }

    /**Orders wordsToUse based on the words average position in the subject line
     *
     * @param wordsToUse
     * @return
     */
    public static String wordsToUseToString(ArrayList<ClusterableWordAndOccurence> wordsToUse){
        String folderName ="";
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
            folderName += aWordsToUse.getWord() + " ";
        }

        return folderName;
    }

    /**Counts the number of occurrences of each word removing common words
     *
     * @param cluster
     * @param max_amount if -1 then returns all else returns at most max_amount
     * @return
     */
    public static ArrayList<ClusterableWordAndOccurence> countWords(Cluster cluster,int max_amount){
        TfidfVectoriser vectoriser = TfidfVectoriser.getVectoriser();
        CachedWordCounter persistentWordCounter = vectoriser.getWordCounter();
        double totalNumberOfDocs = vectoriser.getTotalNumberOfDocs();


        ArrayList<Message> messages = new ArrayList<>();
        TreeMap<String, Integer> wordFrequencySubject = new TreeMap<>(Collections.reverseOrder());
        HashMap<String, Integer> wordTotalPositionMap = new HashMap<>();

        for(ClusterableObject obj : cluster.getContents())
            messages.add(((ClusterableMessage)obj).getMessage());

        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);

            try {
                String[] subjectWords = m.getSubject().toLowerCase().split(" ");
                for (int j = 0; j < subjectWords.length; j++) {
                    if ((persistentWordCounter.frequency(subjectWords[j]) / totalNumberOfDocs)< MAX_OCCURRENCE){//!stopWords.contains(subjectWords[j])) {
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
        while (iterator.hasNext() && (max_amount == -1 || words.size()<max_amount)) {
            Map.Entry<String, Integer> entry = iterator.next();//TODO: Position
            // Calculate Relative positions
            int averagePosition = wordTotalPositionMap.get(entry.getKey())
                    / wordFrequencySubject.get(entry.getKey());
            ClusterableWordAndOccurence w = new ClusterableWordAndOccurence(entry.getKey(), entry.getValue(),averagePosition);
            words.add(new ClusterableWordAndOccurence(entry.getKey(), entry.getValue(),averagePosition));
        }

        return words;
    }
}
