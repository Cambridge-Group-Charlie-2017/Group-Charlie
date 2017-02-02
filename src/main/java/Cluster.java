import java.util.LinkedList;
import java.util.Vector;

/**
 * Created by Ben on 01/02/2017.
 */

public abstract class Cluster {
    protected String clusterName;

    //Returns a value representing how strongly an email fits in a cluster.
    //Method is irrelevant, could be euclidean distance or probability etc. but smaller must mean better match.
    public abstract double matchStrength(Vector<Double> vec);


    void setName(String name) {clusterName = name;}


    //Could possibly just ignore the effect adding an email has on the averages?
    void addVector(Vector<Double> vec) {
        //TODO: re-evaluate average and stdev vectors
    }


    //not sure yet if necessary. Possibly update metadata using email IDs instead?
    //private LinkedList<Vector<Double>> vectors;
    // LinkedList<Vector<Double>> getVectors() {return vectors;}
}
