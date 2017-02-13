package uk.ac.cam.cl.charlie.clustering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
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

    public static void word2VecNaming(GenericCluster cluster) throws Exception{
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


        //Map to array list
        ArrayList<ClusterableWordAndOccurence> words = new ArrayList<>();
        Iterator<Map.Entry<String, Integer>> iterator = wordFrequencySubject.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,Integer> entry = iterator.next();
            ClusterableWordAndOccurence w = new ClusterableWordAndOccurence(entry.getKey(),entry.getValue());
            words.add(new ClusterableWordAndOccurence(entry.getKey(),entry.getValue()));
        }

        GenericEMClusterer clusterer = new GenericEMClusterer();
        ClusterableObjectGroup wordGroup = new ClusterableWordGroup(words);
        GenericClusterGroup clusters = clusterer.run(wordGroup);

        String folderName = "";
        int cuttOff = (int) (messages.size() * MIN_PROPORTION_CORRECT);

        for(int i = 0; i<clusters.size();i++){
            GenericCluster gc = clusters.get(i);
            ArrayList<ClusterableObject> w = gc.getContents();

            //Get most common word in a cluster
            int mostCommonOccurences = 0;
            String mostCommonWord = "";
            for(int j=0; j<w.size();j++){
                ClusterableWordAndOccurence word = (ClusterableWordAndOccurence) w.get(j);
                if(word.getOccurences() > mostCommonOccurences){
                    mostCommonOccurences = word.getOccurences();
                    mostCommonWord = word.getWord();
                }
            }
            folderName += mostCommonWord + " ";
        }


        //Set folderName
        cluster.setName(folderName);
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
