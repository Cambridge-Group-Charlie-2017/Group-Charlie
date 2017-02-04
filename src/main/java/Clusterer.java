import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Vector;

import javax.mail.Message;

/**
 * Created by Ben on 01/02/2017.
 */

/*
* The Clusterer obtains Messages from the mailbox. Which set of emails it obtains depends on the operation.
* When clustering, it obtains all messages in unprotected folders (or which the user hasn't manually moved).
* During classification, it obtains new emails.
*
* During clustering,
 */

public class Clusterer {
    //private Email_Vectoriser vectoriser;
    private ClusteringAlgorithmWrapper cl = new KMeansWrapper();

    private ArrayList<Cluster> clusters;


    void evalClusters(ArrayList<Message> messages) {
        //main method for evaluating clusters.
        //precondition: all Messages in 'message' are clear for clustering i.e. are not in protected folders.
        //call training methods in Vectoriser. If Vectorising model doesn't require training, these will be blank anyway.
        //postcondition: 'clusters' contains the new clustering, and all emails are in their new clusters on the server.

        //TODO: call vectoriser on messages

        //gets centroids and stdevs of clusters.
        try {
            clusters = cl.run(messages);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }
        //TODO: generate and assign cluster names.

        //TODO: update server with new clusters.
        return;
    }

    void classifyNewEmails(ArrayList<Message> messages) {
        //TODO: get vectors for the emails.
        //TODO: For classification, find best cluster for each email using matchStrength().
        //TODO: Update mailbox accordingly.
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
