package uk.ac.cam.cl.charlie.clustering;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.TreeMap;

import javax.mail.Message;
import javax.mail.MessagingException;

import uk.ac.cam.cl.charlie.vec.TextVector;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;

/**
 * Created by Ben on 07/02/2017.
 * @author M Boyce
 */


public class GenericDummyVectoriser {

    private static HashMap<ClusterableObject, TextVector> map = new HashMap<ClusterableObject, TextVector>();
    private static final int dimensionality = 300;
    private static HashMap<ClusterableObject, Integer> actual = new HashMap<ClusterableObject, Integer>();

    //Return vector associated with 'msg'.
    public static TextVector vectorise(Message msg) {
        return map.get(msg);
    }
    //Return vector associated with 'msg'.//TODO:Edit to do correct thing
    public static TextVector vectorise(ClusterableObject msg) {
        return map.get(msg);
    }
    /*
     * Actual vectoriser will likely have a 'train' function which will be called during the clustering process.
     * Subsequent calls to vectorise() will cause the vector to be evaluated based on the new training data.
     * The dummy 'train' function simulated the train() function by assigning each message a vector, and vectorise()
     * returns that vector.
     */
    public static void train(ArrayList<ClusterableObject> clusterableObjects) {
        //assume 5 clusters.
        if (clusterableObjects.size() < 250) {
            System.err.println("Please supply 250 Message objects for testing");
            return;
        }

        //for each cluster
        for (int i = 0; i < 5; i++) {
            double[] mean = new double[dimensionality];
            double[] stdev = new double[dimensionality];
            //generate random mean, variance for each dimension
            for (int j = 0; j < dimensionality; j++) {
                mean[j] = Math.random() * 20;
                stdev[j] = Math.random() * 20; //var or stdev?
            }

            Random rand = new Random();
            //and generate 50 vectors for the cluster
            for (int j = 0; j < 50; j++) {
                double[] arr = new double[dimensionality];
                //each with 300 elements with gaussian distribution, adjusted for mean and variance.
                for (int k = 0; k < dimensionality; k++) {
                    arr[k] = rand.nextGaussian() * stdev[k] + mean[k];
                }
                //and link it to message i*50+j
                map.put(clusterableObjects.get(i*50+j), new TextVector(arr));
                actual.put(clusterableObjects.get(i*50+j), i);
            }
        }
    }


    private static HashSet<String> stopWords = new HashSet<String>(StopWords.getStopWords());

    /*
    public static HashMap<ClusterableWordAndOccurence, TextVector> vectoriseWords(ArrayList<Message> messages) {
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

        //note from Ben: Is this needed in here? Note that DummyVectoriser was only a temp class
        //for use until the vectoriser was implemented. It wasn't meant to be anything more.
        //TODO: Check vectorising strategy used
        VectorisingStrategy vectorisingStrategy = new TfidfVectoriser();

        HashMap<ClusterableWordAndOccurence,TextVector> wordVectorMap = new HashMap<ClusterableWordAndOccurence,TextVector>();
        Iterator<Map.Entry<String, Integer>> iterator = wordFrequencySubject.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String,Integer> entry = iterator.next();
            ClusterableWordAndOccurence w = new ClusterableWordAndOccurence(entry.getKey(),entry.getValue());
            Optional<TextVector> t = vectorisingStrategy.word2vec(entry.getKey());
            if(t.isPresent()) {
                wordVectorMap.put(w, t.get());
            }
        }

        return wordVectorMap;
    }
    */
}
