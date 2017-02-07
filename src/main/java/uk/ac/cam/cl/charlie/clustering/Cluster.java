package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Message;
import java.util.ArrayList;

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
    public ArrayList<Message> getContents() {return contents;}
    public String getName() {return clusterName;}

    //Naming is a separate process to clustering, so the name can be assigned later.
    public void setName(String name) {clusterName = name;}

    public boolean contains(Message msg) {
        return contents.contains(msg);
    }

    /*
     * Abstract method used for testing which cluster is the best match for a specific Message.
     * Actual implementation varies between implementations. For EMCluster, the output is proportional to
     * the Naive Bayes probability of a match, so higher values are better.
     */
    protected abstract double matchStrength(Message msg) throws VectorElementMismatchException;

    //Extract relevant metadata from the initial contents.
    protected Cluster(ArrayList<Message> initialContents) {
        contents = initialContents;
        clusterSize = initialContents.size();
        dimensionality = DummyVectoriser.vectorise(initialContents.get(0)).size();
    }

    protected abstract void updateMetadataAfterAdding(Message msg);

    //adding a new message to a clustering (during classification) should cause clustering metadata to change accordingly.
    public void addMessage(Message msg) {
        updateMetadataAfterAdding(msg);
        contents.add(msg);
        clusterSize++;
    }

    //updateServer() method? Could take mailbox as argument and use Mailbox to update server. Update could also be in
    //addMessage()
}
