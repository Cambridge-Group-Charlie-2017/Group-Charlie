import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
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


        //TODO: Now loop over emails, call vectoriser on each and then classifyEmail().
        //TODO: At the same time, try to identify new cluster names.
        return;
    }

    void classifyNewEmails() {
        //TODO: get new emails, call vectoriser, then call classifyEmail() on it.
    }

    //Should have an argument. Not sure yet if we should use email ID or Email object.
    void classifyEmail() {
        //TODO: For classification, find closest centroid for an email using matchStrength() in Cluster.
        //TODO: Impose whatever restrictions you like, for example, if KMeans is used you could put an email
        //TODO: in default 'inbox' if two highest distances are too similar. If EM used, Possibly classify
        //TODO: as 'inbox' if no probability exceeds a threshold.
    }


    //Temp code for generating test vector file.
    public static void main(String args[]) throws Exception{

        FileWriter writer = new FileWriter("testVecs.arff");
        BufferedReader in = new BufferedReader(new FileReader("iris.arff"));
        writer.write("@RELATION vectors \n");
        for (int i = 0; i < 4; i++) {
            writer.write("@ATTRIBUTE e" + i + " \n");
        }
        writer.write("\n@DATA\n");

        String currLine;
        try {
            int i = 0;
            while (i < 299) {
                currLine = in.readLine();
                if (currLine == null)
                    break;
                if (!currLine.equals("")) {
                    String[] tokens = currLine.split(",");
                    currLine = tokens[0]+","+tokens[1]+","+tokens[2]+","+tokens[3];
                    writer.write(currLine+"\n");
                    writer.flush();
                }
            }
        } catch (Exception e) {}
        writer.close();
        in.close();
    }

}
