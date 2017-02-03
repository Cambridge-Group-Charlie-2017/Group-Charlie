import javax.mail.Message;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Created by Ben on 01/02/2017.
 */

public abstract class Cluster {
    protected String clusterName;
    protected ArrayList<Message> contents;
    int dimensionality;
    int clusterSize;

    //Returns a value representing how strongly an email fits in a cluster.
    //Method is irrelevant, could be euclidean distance or probability etc. but smaller must mean better match.
    public abstract double matchStrength(Vector<Double> vec) throws VectorElementMismatchException;


    void setName(String name) {clusterName = name;}

    //adding a new message to a cluster (during classification) should cause cluster metadata to change accordingly.
    abstract void addMessage(Message msg);

    ArrayList<Message> getContents() {return contents;}
}
