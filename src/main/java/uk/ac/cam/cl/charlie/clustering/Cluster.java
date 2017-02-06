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
    int dimensionality;
    int clusterSize;

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
        //TODO: initialise dimensionality
    }

    //adding a new message to a clustering (during classification) should cause clustering metadata to change accordingly.
    public abstract void addMessage(Message msg);

    //updateServer() method? Could take mailbox as argument and use Mailbox to update server.

    ArrayList<Message> getContents() {return contents;}
}
