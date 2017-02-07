package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;

/**
 * Created by Ben on 07/02/2017.
 */


public class DummyVectoriser {

    private static HashMap<Message, Vector<Double>> map = new HashMap<Message, Vector<Double>>();
    private static final int dimensionality = 300;
    private static HashMap<Message, Integer> actual = new HashMap<Message, Integer>();

    //Return vector associated with 'msg'.
    public static Vector<Double> vectorise(Message msg) {
        return map.get(msg);
    }

    /*
     * Actual vectoriser will likely have a 'train' function which will be called during the clustering process.
     * Subsequent calls to vectorise() will cause the vector to be evaluated based on the new training data.
     * The dummy 'train' function simulated the train() function by assigning each message a vector, and vectorise()
     * returns that vector.
     */
    public static void train(ArrayList<Message> messages) {
        //assume 5 clusters.
        if (messages.size() < 250) {
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
                Vector<Double> vec = new Vector<Double>();
                //each with 300 elements with gaussian distribution, adjusted for mean and variance.
                for (int k = 0; k < dimensionality; k++) {
                    vec.add(rand.nextDouble() * stdev[k] + mean[k]);
                }
                //and link it to message i*50+j
                map.put(messages.get(i*50+j), vec);
                actual.put(messages.get(i*50+j), i);
            }
        }
    }
}
