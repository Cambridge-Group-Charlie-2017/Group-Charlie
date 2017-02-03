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
        //TODO: once vectoriser implemented, replace with genuine getVec method.
        Vector<Double> vec = testGetVec(msg);

        for (int i = 0; i < dimensionality; i++) {
            double newAvg = (clusterSize * centroid.get(i) + vec.get(i)) / (clusterSize + 1);
            centroid.set(i,newAvg);
        }
    }

    public double matchStrength(Vector<Double> vec) throws VectorElementMismatchException{
        if (vec.size() != centroid.size())
            throw new VectorElementMismatchException();

        //returns square of distance
        double distanceSquared = 0;
        for (int i = 0; i < centroid.size(); i++) {
            distanceSquared += Math.pow(centroid.get(i) - vec.get(i), 2.0);
        }
        return distanceSquared;
    }

    public KMeansCluster(Vector<Double> cent, ArrayList<Message> initialContents) {
        this.centroid = cent;
        this.contents = initialContents;
    }

    //Temp test function. Delete when Vectoriser implemented.
    private Vector<Double> testGetVec(Message msg) {
        Vector<Double> vec = new Vector<Double>();
        for (int i = 0; i < dimensionality; i++) {
            vec.add((double)i);
        }
        return vec;
    }
}
