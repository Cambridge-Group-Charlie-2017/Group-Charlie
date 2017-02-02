import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by Ben on 01/02/2017.
 */

/*
* The Clusterer obtains emails from the Mailbox,
 */

public class Clusterer {
    //private Email_Vectoriser vectoriser;
    private ClusteringAlgorithmWrapper cl = new KMeansWrapper();

    private Email[] emailBuffer;
    private ArrayList<Cluster> clusters;

    void initialClusters(ArrayList<Vector<Double>> vecs) {
        //TODO: call vectoriser on all emails. This should only run once on the user's pre-existing set of emails.
        evalClusters(vecs);
        return;
    }

    void evalClusters(ArrayList<Vector<Double>> vecs) {
        //main method for evaluating clusters.
        //All emails requiring clustering should already be clustered.

        //gets centroids and stdevs of clusters.
        try {
            clusters = cl.run(vecs);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }


        //TODO: Now loop over emails, for each assess closeness to each centroid and
        return;
    }

    void classifyNewEmails() {
        //TODO: for classification, find closest centroid for an email.
        //TODO: Once one's identified, THEN use naive Bayes to find the probability
    }



}
