package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.Random;
import java.util.Vector;

/**
 * Created by Ben on 05/02/2017.
 */
public class EMCluster extends Cluster {

    private Vector<Double> average;
    private Vector<Double> variance;

    public EMCluster(ArrayList<Message> messages) {
        super(messages);

        //calculate avg and variance.
        int dimensionality = getDimensionality();
        int n = getClusterSize();

        ArrayList<Vector<Double>> vecs = new ArrayList<Vector<Double>>();
        for (int i = 0; i < n; i++)
            vecs.add(DummyVectoriser.vectorise(messages.get(i)));

        //initialise average and variance vectors.
        average = new Vector<Double>();
        variance = new Vector<Double>();
        for (int i = 0; i < dimensionality; i++) {
            double sumOfSquares = 0;
            double sum = 0;
            for (int j = 0; j < n; j++) {
                double xi = vecs.get(j).get(i);
                sum += xi;
                sumOfSquares += (xi * xi);
            }
            double avgX = sum / n;
            double varX = ((n - 1) / (double)n) * ((sumOfSquares / n) - avgX * avgX);
            average.add(i, avgX);
            variance.add(i, varX);
        }
    }

    @Override
    protected void updateMetadataAfterAdding(Message msg) {
        //changes variance and average arrays.
        //once vectoriser implemented, replace with genuine getVec method.
        Vector<Double> vec = testGetVec(msg);

        //Note: clustersize has not been incremented yet at this point.
        for (int i = 0; i < getDimensionality(); i++) {
            double newAvg = (getClusterSize() * average.get(i) + vec.get(i)) / (getClusterSize() + 1);
            average.set(i,newAvg);
        }

        //Recalculating variance would be expensive. Maybe best not bother and assume it stays constant.
    }

    public double matchStrength (Vector<Double> vec) throws VectorElementMismatchException{
        //return weighted Bayes probability. Assume each category has equal probability.
        //Note it's not the actual probability - that would require multiplying by irrational numbers, and there's
        //no point. As long as all probabilities are off by the same factor, comparison still works.

        //Note: at high dimensionality, all probs are really small. Could possibly randomly select vector elements instead?
        if (vec.size() != getDimensionality())
            throw new VectorElementMismatchException();
        double prob = 1;
        for (int i = 0; i < getDimensionality(); i++) {
            double diff = vec.get(i) - average.get(i);
            double temp = -(diff * diff) / (2 * variance.get(i));
            double probi = Math.exp(temp) / Math.sqrt(variance.get(i));
            prob *= probi;
        }
        return prob;
    }




    //temp testing function, use until vectoriser is implemented.
    public ArrayList<Vector<Double>> getTestVecs(int num) {
        ArrayList<Vector<Double>> vecs = new ArrayList<Vector<Double>>();

        for (int i = 0; i < num; i++) {
            Vector<Double> vec = new Vector<Double>();
            for (int j = 0; j < getDimensionality(); j++) {
                vec.add(Math.random() * 10);
            }
            vecs.add(i, vec);
        }
        return vecs;
    }

}
