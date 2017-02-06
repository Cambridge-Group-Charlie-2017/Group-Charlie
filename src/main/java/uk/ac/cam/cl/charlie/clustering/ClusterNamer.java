package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.util.*;

/**
 * Created by M Boyce on 04/02/2017.
 */
public class ClusterNamer {
    static HashSet<String> stopWords = StopWords.getStopWords();

    /**
     * Given an cluster of emails generates a name for the cluster based on the emails in that
     * clusters contents and sets the given clusters name to that
     * @param cluster
     */
    public static void name(Cluster cluster){
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
}
