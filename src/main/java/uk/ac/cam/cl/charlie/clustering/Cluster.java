package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by Ben on 01/02/2017.
 */

public abstract class Cluster {
    protected String clusterName;
    protected ArrayList<Message> contents;
    private int dimensionality;
    private int clusterSize;

    public int getDimensionality() {return dimensionality;}
    public int getClusterSize() {return clusterSize;}

    //Returns a value representing how strongly an email fits in a clustering.
    //Method is irrelevant, could be euclidean distance or probability etc. but smaller must mean better match.
    public abstract double matchStrength(Vector<Double> vec) throws VectorElementMismatchException;

    void setName(String name) {clusterName = name;}

    public String getName(){
        return clusterName;
    }

    protected Cluster(ArrayList<Message> initialContents) {
        contents = initialContents;
        clusterSize = initialContents.size();
        //Following statement for finding dimensionality should be replaced once vectoriser is implemented.
        dimensionality = testGetVec(initialContents.get(0)).size();
    }

    protected abstract void updateMetadataAfterAdding(Message msg);

    //adding a new message to a clustering (during classification) should cause clustering metadata to change accordingly.
    public void addMessage(Message msg) {
        updateMetadataAfterAdding(msg);
        clusterSize++;
    }

    //updateServer() method? Could take mailbox as argument and use Mailbox to update server.

    ArrayList<Message> getContents() {return contents;}

    //temp test function, for use until Vectoriser is implemented.
    protected Vector<Double> testGetVec(Message message) {

        int dimensionality = 300;
        Vector<Double> v = new Vector<Double>();
        for (int j = 0; j < dimensionality; j++) {
            v.add(java.lang.Math.random() * 4);
        }

        return v;
    }
}
