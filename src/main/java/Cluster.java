import java.util.LinkedList;
import java.util.Vector;

/**
 * Created by Ben on 01/02/2017.
 */

public class Cluster {
    private String clusterName;
    //private LinkedList<Vector<Double>> vectors;
    private Vector<Double> centroid;
    private Vector<Double> stdevs;

    Vector<Double> getCentroid() {return centroid;}
    void addVector(Vector<Double> vec) {
        //TODO: re-evaluate average and stdev vectors
    }
    Vector<Double> getStdevs() {return stdevs;}

    void setName(String name) {clusterName = name;}

    public Cluster(Vector<Double> cnt, Vector<Double> std) {
        centroid = cnt;
        stdevs = std;
    }

    public double getProb(Vector<Double> vec) {
        //TODO: Use Bayes' classification to identify probability of membership
        return 0;
    }

    public double getDistance(Vector<Double> vec) {
        //TODO: Find the distance to the centroid. Useful for KMeans or XMeans clusters
        return 0;
    }

    //not sure yet if necessary. Possibly list of email IDs instead?
    //LinkedList<Vector<Double>> getVectors() {return vectors;}
}
