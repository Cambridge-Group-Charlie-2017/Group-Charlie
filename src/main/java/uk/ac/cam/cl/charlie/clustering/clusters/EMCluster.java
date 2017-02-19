
package uk.ac.cam.cl.charlie.clustering.clusters;

import java.util.ArrayList;
import java.util.Vector;

import uk.ac.cam.cl.charlie.clustering.*;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.vec.BatchSizeTooSmallException;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;

/*
 * Created by Ben on 05/02/2017.
 */
public class EMCluster extends Cluster {

    //vectors storing the current mean and variance vectors of the messages associated with this cluster.
    private Vector<Double> average;
    private Vector<Double> variance;


    // Only constructor for EMCluster. Requires initial contents with which the cluster will be initialised.
    public EMCluster(ArrayList<ClusterableObject> messages) {
        super(messages);

        int dimensionality = getDimensionality();
        int n = getClusterSize();

        //Get the vectors for the messages.
        uk.ac.cam.cl.charlie.math.Vector[] vecs = getContentVecs().toArray(new uk.ac.cam.cl.charlie.math.Vector[0]);

        //initialise average and variance vectors using message vectors.
        average = new Vector<Double>();
        variance = new Vector<Double>();
        for (int i = 0; i < dimensionality; i++) {
            double sumOfSquares = 0;
            double sum = 0;
            //Calculate sum and sum of squares over all vectors for element i
            for (int j = 0; j < n; j++) {
                double xi = vecs[j].get(i);
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
    protected void updateMetadataAfterAdding(ClusterableObject msg) {
        //changes variance and average arrays.
        //once vectoriser implemented, replace with genuine getVec method.

        VectorisingStrategy vectoriser = EMClusterer.getVectoriser();

        //Note: clustersize has not been incremented yet at this point.
        try {
            for (int i = 0; i < getDimensionality(); i++) {

                double newAvg = (getClusterSize() * average.get(i)
                   + vectoriser.doc2vec(((ClusterableMessage)msg).getMessage()).get(i)) / (getClusterSize() + 1);
                average.set(i,newAvg);
            }
        } catch (BatchSizeTooSmallException e) {
            return; //If msg can't be vectorised, no metadata can be updated.
        }

        //Recalculating variance would be expensive. Maybe best not bother and assume it stays constant.
    }

    public boolean isHighMatchGood() {return true;}
    private final int elementsToCompare = 100;

    @Override
    public double matchStrength (ClusterableObject msg) throws IncompatibleDimensionalityException {
        //return weighted Bayes probability. Assumes each category has equal probability.
        //Note it's not the actual probability - that would require multiplying by irrational numbers, and there's
        //no point. As long as all probabilities are off by the same factor, comparison still works.

        //only consider a subset of elements. if all probabilities are multiplied then the resulting
        //probability becomes too small at high dimensions.

        //TODO: put in log space
	uk.ac.cam.cl.charlie.math.Vector vec;
        try {
            vec = Clusterer.getVectoriser().doc2vec(((ClusterableMessage)msg).getMessage());
        } catch (BatchSizeTooSmallException e) {
            return Integer.MAX_VALUE;
        }

        if (vec.size() != getDimensionality())
            throw new IncompatibleDimensionalityException();

        int dimensionality = getDimensionality();
        int interval = vec.size() / elementsToCompare;
        double logProb = 0;
        for (int i = 0; i < dimensionality; i+= interval) {
            double diff = vec.get(i) - average.get(i);

            //Calculate Gaussian probability of membership of this cluster based on element i
            logProb += Math.log(Math.exp(-(diff * diff) / (2.0 * variance.get(i))) / Math.sqrt(2*Math.PI*variance.get(i)));

            //Add all logs together for log naive Bayes probability
        }
        return logProb;

        //TODO: This method doesn't seem very reliable. Possibly could use a classifer instead, but less efficient.
    }
}
