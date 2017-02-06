package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by Ben on 02/02/2017.
 */
public class KMeansCluster extends Cluster{
    protected Vector<Double> centroid;
    Vector<Double> getCentroid() {return centroid;}

    protected void updateMetadataAfterAdding(Message msg) {
        //once vectoriser implemented, replace with genuine getVec method.
        Vector<Double> vec = testGetVec(msg);

        //Note: clustersize has not been incremented yet at this point.
        for (int i = 0; i < getDimensionality(); i++) {
            double newAvg = (getClusterSize() * centroid.get(i) + vec.get(i)) / (getClusterSize() + 1);
            centroid.set(i,newAvg);
        }
    }

    public double matchStrength(Vector<Double> vec) throws VectorElementMismatchException {
        if (vec.size() != centroid.size())
            throw new VectorElementMismatchException();

        //returns square of distance
        double distanceSquared = 0;
        for (int i = 0; i < centroid.size(); i++) {
            distanceSquared += Math.pow(centroid.get(i) - vec.get(i), 2.0);
        }
        return distanceSquared;
    }

    public KMeansCluster(Vector<Double> initialCentroid, ArrayList<Message> initialContents) {
        super(initialContents);
        this.centroid = initialCentroid;
    }


}
