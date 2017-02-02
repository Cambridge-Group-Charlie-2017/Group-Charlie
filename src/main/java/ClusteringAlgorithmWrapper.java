import weka.clusterers.*;

import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by Ben on 01/02/2017.
 */
public abstract class ClusteringAlgorithmWrapper {
    //Possible implentations:
    //KMeans, XMeans, EM, etc.
    ArrayList<Cluster> clusters = new ArrayList<Cluster>();

    public abstract ArrayList<Cluster> run(ArrayList<Vector<Double>> vecs) throws Exception;

    private void genClusterNames() {

    }
}
