import weka.clusterers.*;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by Ben on 01/02/2017.
 */
public abstract class ClusteringAlgorithmWrapper {
    //Possible implentations:
    //KMeans, XMeans, EM, etc.
    ArrayList<Cluster> clusters = new ArrayList<Cluster>();

    //Only identifies cluster metadata. Clusterer will actually assign emails based on the metadata.
    public abstract ArrayList<Cluster> run(ArrayList<Message> vecs) throws Exception;
}
