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

    public void addMessage(Message msg) {
        //once vectoriser implemented, replace with genuine getVec method.
        Vector<Double> vec = testGetVec(msg);

        for (int i = 0; i < dimensionality; i++) {
            double newAvg = (clusterSize * centroid.get(i) + vec.get(i)) / (clusterSize + 1);
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

    //temp test function, for use until Vectoriser is implemented.
    private Vector<Double> testGetVec(Message message) {

        int dimensionality = 5;
        Vector<Double> v = new Vector<Double>();
        for (int j = 0; j < dimensionality; j++) {
            v.add(java.lang.Math.random() * 3);
        }

        return v;
    }
}
