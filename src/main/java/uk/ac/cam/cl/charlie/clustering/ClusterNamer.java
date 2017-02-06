package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.*;
import org.deeplearning4j.text.stopwords.StopWords;
/**
 * Created by M Boyce on 04/02/2017.
 */
public class ClusterNamer {
    private static HashSet<String> stopWords = new HashSet<String>();//StopWords.getStopWords());
    private static double MIN_PROPRTION_CORRECT = 0.8;

    /**
     * Given an cluster of emails generates and sets the name for the cluster based on the emails in that
     * clusters contents and sets the given clusters name to that
     * @param cluster
     */
    public static void basicNaming(Cluster cluster){
        ArrayList<Message> messages = cluster.getContents();

        TreeMap<String, Integer> wordFrequencySubject = new TreeMap<String, Integer>();
        //TODO: Possibly include body of email in naming
        //TreeMap<String,Integer> wordFrequencyBody = new TreeMap<String,Integer>();


        //Loop through all emails in a cluster and find word frequencies
        for(int i = 0; i<messages.size();i++){
            Message m = messages.get(i);

            try {
                String[] subjectWords = m.getSubject().toLowerCase().split(" ");
                for (int j = 0; j < subjectWords.length; j++) {
                    if (!stopWords.contains(subjectWords[j])) {
                        int count = wordFrequencySubject.get(subjectWords[j]);
                        wordFrequencySubject.put(subjectWords[j], count + 1);
                    }
                }

                //Return most common word

            } catch (MessagingException e) {
                System.out.println(e.getMessage());

            }

        }

        //Set cluster name
        if(wordFrequencySubject.lastKey() != null)
            cluster.setName(wordFrequencySubject.lastKey());
        else
            cluster.setName("Error Naming Cluster");
    }

    /**
     * Given a cluster of emails generates and sets the name for the cluster based on the sender
     * of the emails in that cluster
     * @param cluster
     */
    public static void senderNaming(Cluster cluster) throws ClusterNamingException {
        List<Message> messages = new ArrayList<Message>();
        messages = cluster.getContents();

        //Map storing number of occurrences of each domain name in the sender address of the messages in the cluster
        TreeMap<String, Integer> domains = new TreeMap<String, Integer>();

        for(int i=0; i<messages.size(); i++){
            try {
                Address[] from = messages.get(i).getFrom();
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
        if(mostCommonDomain.getValue()/messages.size() > MIN_PROPRTION_CORRECT){
            cluster.setName(mostCommonDomain.getKey());
        }else{
            throw new ClusterNamingException("Cannot use sender as folder name");
        }
    }
}
