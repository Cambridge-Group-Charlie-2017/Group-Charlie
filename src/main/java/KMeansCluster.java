import java.util.Vector;

/**
 * Created by Ben on 02/02/2017.
 */
public class KMeansCluster extends Cluster{
    protected Vector<Double> centroid;
    Vector<Double> getCentroid() {return centroid;}

    public double matchStrength(Vector<Double> vec) {
        //TODO: find distance
        return 0;
    }

    public KMeansCluster(Vector<Double> cent) {
        this.centroid = cent;
    }
}
