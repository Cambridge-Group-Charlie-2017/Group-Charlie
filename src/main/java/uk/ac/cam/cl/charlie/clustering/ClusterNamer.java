package uk.ac.cam.cl.charlie.clustering;

import javafx.collections.FXCollections;
import uk.ac.cam.cl.charlie.vec.TextVector;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;
import uk.ac.cam.cl.charlie.vec.Word;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.*;
/**
 * @author M Boyce
 */
public class ClusterNamer {
    private static HashSet<String> stopWords = new HashSet<String>(StopWords.getStopWords());

    private static double MIN_PROPORTION_CORRECT = 0.8;

    /**
     * Given an cluster of emails generates and sets the name for the cluster based on the emails in that
     * clusters contents and sets the given clusters name to that
     * @param cluster
     */
    public static void subjectNaming(GenericCluster cluster) throws ClusterNamingException {
        ArrayList<ClusterableObject> messages = cluster.getContents();

        TreeMap<String, Integer> wordFrequencySubject = new TreeMap<String, Integer>(Collections.reverseOrder());
        //TODO: Possibly include body of email in naming
        //TreeMap<String,Integer> wordFrequencyBody = new TreeMap<String,Integer>();


        //Loop through all emails in a cluster and find word frequencies
        for(int i = 0; i<messages.size();i++){
            Message m = ((ClusterableMessage) messages.get(i)).getMessage();

            try {
                String[] subjectWords = m.getSubject().toLowerCase().split(" ");
                for (int j = 0; j < subjectWords.length; j++) {
                    if (!stopWords.contains(subjectWords[j])) {
                        int count = 0;
                        if(wordFrequencySubject.containsKey(subjectWords[j])){
                            count = wordFrequencySubject.get(subjectWords[j]);
                        }
                        wordFrequencySubject.put(subjectWords[j], count + 1);
                    }
                }

                //Return most common word

            } catch (MessagingException e) {
                System.out.println(e.getMessage());

            }

        }

        //Generate cluster name
        String clusterName = "";
        ArrayList<String> wordsToUse = new ArrayList<String>();
        SortedMap<String,Integer> sortedMap = wordFrequencySubject.descendingMap();
        Iterator<Map.Entry<String, Integer>> iterator = wordFrequencySubject.entrySet().iterator();

        //Generate cut off for number of occurrences
        int cutOff = (int) (messages.size()*MIN_PROPORTION_CORRECT);


        Map.Entry<String,Integer> curEntry = iterator.next();
        while(curEntry != null && curEntry.getValue() > cutOff){
            clusterName  += " " + curEntry.getKey();
            curEntry = iterator.next();
        }

        //Set cluster name
        if(cluster != null)
            cluster.setName(clusterName);
        else {
            cluster.setName("Error Naming Cluster");
            throw new ClusterNamingException("Basic naming failed");
        }
    }

    /**
     * Given a cluster of emails generates and sets the name for the cluster based on the sender
     * of the emails in that cluster
     * @param cluster
     */
    public static void senderNaming(GenericCluster cluster) throws ClusterNamingException {
        ArrayList<ClusterableObject> messages = cluster.getContents();

        //Map storing number of occurrences of each domain name in the sender address of the messages in the cluster
        TreeMap<String, Integer> domains = new TreeMap<String, Integer>();

        for(int i=0; i<messages.size(); i++){
            try {
                Address[] from = ((ClusterableMessage)messages.get(i)).getMessage().getFrom();
                String[] address = from[0].toString().split("@");

                //Remove the .com /.net ect
                String[] tempDomain = address[1].split("\\.");
                String domain="";

                for(int j=0; j<tempDomain.length-1;j++){
                    domain += tempDomain[j];
                }

                //Increment the count of relevant domain
                int count = 0;
                if(domains.containsKey(domain)){
                    count = domains.get(domain);
                }
                domains.put(domain, count + 1);

            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }

        //Test to see if is a good name for cluster by seeing what proportion of the Cluster this name holds for
        Map.Entry<String,Integer> mostCommonDomain = domains.lastEntry();
        if(mostCommonDomain.getValue()/messages.size() > MIN_PROPORTION_CORRECT){
            cluster.setName(mostCommonDomain.getKey());
        }else{
            throw new ClusterNamingException("Cannot use sender as folder name");
        }
    }

    /**
     * Given a cluster of emails generates and sets the name for the cluster using a textRank based algorithm
     * (Takes into account how related sentences in the emails are)
     * @param cluster
     */
    public static void textRankNaming(GenericCluster cluster){

    }

    public static void word2VecNaming(GenericCluster cluster) {
        ArrayList<Message> messages = new ArrayList<Message>();
        TreeMap<String, Integer> wordFrequencySubject = new TreeMap<String, Integer>(Collections.reverseOrder());

        for (int i = 0; i < messages.size(); i++) {
            Message m = messages.get(i);

            try {
                String[] subjectWords = m.getSubject().toLowerCase().split(" ");
                for (int j = 0; j < subjectWords.length; j++) {
                    if (!stopWords.contains(subjectWords[j])) {
                        int count = 0;
                        if (wordFrequencySubject.containsKey(subjectWords[j])) {
                            count = wordFrequencySubject.get(subjectWords[j]);
                        }
                        wordFrequencySubject.put(subjectWords[j], count + 1);
                    }
                }

            } catch (MessagingException e) {
                System.out.println(e.getMessage());

            }
        }

        //TODO: Check vectorising strategy used
        VectorisingStrategy vectorisingStrategy = new TfidfVectoriser();


        HashMap<WordAndOccurences,TextVector> wordVectorMap = new HashMap<WordAndOccurences,TextVector>();
        Iterator<Map.Entry<String, Integer>> iterator = wordFrequencySubject.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,Integer> entry = iterator.next();
            WordAndOccurences w = new WordAndOccurences(entry.getKey(),entry.getValue());
            Optional<TextVector> t = vectorisingStrategy.word2vec(entry.getKey());
            if(t.isPresent()) {
                wordVectorMap.put(w, t.get());
            }
        }

        GenericEMClusterer clusterer = new GenericEMClusterer();

    }

    /**
     * Generic naming of the cluster that tries simple methods first aand if they are not good enough try more complicated
     * methods
     * @param cluster
     */
    public static void name(GenericCluster cluster){
        try{
            senderNaming(cluster);
        } catch (ClusterNamingException e) {
            //Sender naming method is not good enough
            try {
                subjectNaming(cluster);
            } catch (ClusterNamingException e1) {
                //subjectNaming method is not good enough
                e1.printStackTrace();
            }
        }
    }
}
