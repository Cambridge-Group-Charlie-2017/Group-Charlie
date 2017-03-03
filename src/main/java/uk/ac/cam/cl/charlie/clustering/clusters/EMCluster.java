
package uk.ac.cam.cl.charlie.clustering.clusters;

import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.cl.charlie.clustering.IncompatibleDimensionalityException;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.math.Vector;

/*
 * Created by Ben on 05/02/2017.
 */
public class EMCluster<T> extends Cluster<T> {

    // vectors storing the current mean and variance vectors of the messages
    // associated with this cluster.
    private Vector average;
    private Vector variance;

    // Only constructor for EMCluster. Requires initial contents with which the
    // cluster will be initialised.
    public EMCluster(ArrayList<ClusterableObject<T>> messages) {
        super(messages);

        int dimension = getDimension();
        int n = getSize();

        // Get the vectors for the messages.
        List<Vector> vecs = getVectors();

        // initialise average and variance vectors using message vectors.
        double[] averageData = new double[dimension];
        double[] varianceData = new double[dimension];
        for (int i = 0; i < dimension; i++) {
            double sumOfSquares = 0;
            double sum = 0;
            // Calculate sum and sum of squares over all vectors for element i
            for (int j = 0; j < n; j++) {
                double xi = vecs.get(j).get(i);
                sum += xi;
                sumOfSquares += (xi * xi);
            }
            // calculate mean and variance of element i
            double avgX = sum / n;
            double varX = ((double) n / (n - 1)) * ((sumOfSquares / n) - avgX * avgX);
            averageData[i] = avgX;
            varianceData[i] = varX;
        }

        average = new Vector(averageData);
        variance = new Vector(varianceData);
    }

    /*
     * Called by the addMessage() method in Cluster. This function updates the
     * metadata associated with this cluster. For this EMCluster implementation,
     * the metadata is the average and variance vectors. 'msg' is the newly
     * added message.
     */
    @Override
    protected void updateMetadataAfterAdding(ClusterableObject<T> msg) {
        // changes variance and average arrays.
        // Note: size has not been incremented yet at this point.
        average = average.scale(getSize()).add(msg.getVector()).scale(1 / (getSize() + 1));

        // Recalculating variance would be expensive. Maybe best not bother and
        // assume it stays constant.
    }

    @Override
    public boolean isHighMatchGood() {
        return true;
    }

    @Override
    public double matchStrength(ClusterableObject<T> msg) throws IncompatibleDimensionalityException {
        // return log of weighted Bayes probability. Assumes each category has
        // equal probability.
        // Note it's not the actual probability - that would require multiplying
        // by irrational numbers, and there's
        // no point. As long as all probabilities are off by the same factor,
        // comparison still works.

        // only consider a subset of elements. if all probabilities are
        // multiplied then the resulting
        // probability becomes too small at high dimensions.
        Vector vec = msg.getVector();

        if (vec.size() != getDimension())
            throw new IncompatibleDimensionalityException();

        int dimension = getDimension();
        double logProb = 0;
        for (int i = 0; i < dimension; i++) {
            double diff = vec.get(i) - average.get(i);

            // Calculate Gaussian probability of membership of this cluster
            // based on element i
            double prob = Math.exp(-(diff * diff) / (2.0 * variance.get(i))) / Math.sqrt(2 * Math.PI * variance.get(i));

            // Add all logs together for log naive Bayes probability. (More
            // stable than multiplication)
            logProb += Math.log(prob);
        }
        return logProb;
    }
}
