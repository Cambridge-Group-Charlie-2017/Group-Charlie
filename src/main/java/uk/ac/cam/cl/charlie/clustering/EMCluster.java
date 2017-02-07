package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

/**
 * Created by Ben on 05/02/2017.
 */
public class EMCluster extends Cluster {

    //vectors storing the current mean and variance vectors of the messages associated with this cluster.
    private Vector<Double> average;
    private Vector<Double> variance;


    // Only constructor for EMCluster. Requires initial contents with which the cluster will be initialised.
    public EMCluster(ArrayList<Message> messages) {
        super(messages);

        int dimensionality = getDimensionality();
        int n = getClusterSize();

        //Get the vectors for the messages.
        ArrayList<Vector<Double>> vecs = new ArrayList<Vector<Double>>();
        for (int i = 0; i < n; i++)
            vecs.add(DummyVectoriser.vectorise(messages.get(i)));

        //initialise average and variance vectors using message vectors.
        average = new Vector<Double>();
        variance = new Vector<Double>();
        for (int i = 0; i < dimensionality; i++) {
            double sumOfSquares = 0;
            double sum = 0;
            //Calculate sum and sum of squares over all vectors for element i
            for (int j = 0; j < n; j++) {
                double xi = vecs.get(j).get(i);
                sum += xi;
                sumOfSquares += (xi * xi);
            }
            //calculate mean and variance of element i
            double avgX = sum / n;
            double varX = ((n - 1) / (double)n) * ((sumOfSquares / n) - avgX * avgX);
            average.add(i, avgX);
            variance.add(i, varX);
        }
    }

    /*
     * Called by the addMessage() method in Cluster. This function updates the metadata associated with this cluster.
     * For this EMCluster implementation, the metadata is the average and variance vectors.
     * 'msg' is the newly added message.
     */
    @Override
    protected void updateMetadataAfterAdding(Message msg) {
        //changes variance and average arrays.
        //once vectoriser implemented, replace with genuine getVec method.
        Vector<Double> vec = DummyVectoriser.vectorise(msg);

        //Note: clustersize has not been incremented yet at this point.
        for (int i = 0; i < getDimensionality(); i++) {
            double newAvg = (getClusterSize() * average.get(i) + vec.get(i)) / (getClusterSize() + 1);
            average.set(i,newAvg);
        }

        //Recalculating variance would be expensive. Maybe best not bother and assume it stays constant.
    }

    private final int elementsToCompare = 300;
    @Override
    public double matchStrength (Message msg) throws VectorElementMismatchException{
        //return weighted Bayes probability. Assumes each category has equal probability.
        //Note it's not the actual probability - that would require multiplying by irrational numbers, and there's
        //no point. As long as all probabilities are off by the same factor, comparison still works.

        //only consider a subset of elements. if all probabilities are multiplied then the resulting
        //probability becomes too small at high dimensions.
        Vector<Double> vec = DummyVectoriser.vectorise(msg);
        if (vec.size() != getDimensionality())
            throw new VectorElementMismatchException();

        int dimensionality = getDimensionality();
        int interval = vec.size() / elementsToCompare;
        double prob = 1;
        for (int i = 0; i < dimensionality; i+= interval) {
            double diff = vec.get(i) - average.get(i);

            //Calculate Gaussian probability of membership of this cluster based on element i
            double probi = Math.exp(-(diff * diff) / (2.0 * variance.get(i))) / Math.sqrt(variance.get(i));

            //Multiply all elements together for naive Bayes probability
            prob *= probi;
        }
        return prob;

        //TODO: This method doesn't seem very reliable. Possibly could use a classifer instead, but less efficient.
    }
}
