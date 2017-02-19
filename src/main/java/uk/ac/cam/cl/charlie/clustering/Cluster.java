package uk.ac.cam.cl.charlie.clustering;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;

import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.BatchSizeTooSmallException;

public abstract class Cluster {

    protected String clusterName;
    protected ArrayList<ClusterableObject> contents;
    private int dimensionality;
    private int clusterSize;
    // TODO: store vectors. If vector not there, vectorise.

    public int getDimensionality() {
	return dimensionality;
    }

    public int getClusterSize() {
	return clusterSize;
    }

    public ArrayList<ClusterableObject> getContents() {
	return contents;
    }

    public String getName() {
	return clusterName;
    }

    // Naming is a separate process to clustering, so the name can be assigned
    // later.
    public void setName(String name) {
	clusterName = name;
    }

    public boolean contains(ClusterableObject obj) {
	return contents.contains(obj);
    }

    public boolean containsMessage(Message msg) {
	return contents.contains(new ClusterableMessage(msg));
    }

    /*
     * Abstract method used for testing which cluster is the best match for a
     * specific Message. Actual implementation varies between implementations.
     * For EMCluster, the output is proportional to the Naive Bayes probability
     * of a match, so higher values are better.
     */
    protected abstract double matchStrength(ClusterableObject msg) throws IncompatibleDimensionalityException;

    public abstract boolean isHighMatchGood();

    // Extract relevant metadata from the initial contents.
    protected Cluster(ArrayList<ClusterableObject> initialContents) {
	contents = initialContents;
	clusterSize = initialContents.size();
	dimensionality = initialContents.get(0).getVec().size();
    }

    protected abstract void updateMetadataAfterAdding(ClusterableObject msg);

    // adding a new message to a clustering (during classification) should cause
    // clustering metadata to change accordingly.
    public void addMessage(ClusterableObject msg) {
	updateMetadataAfterAdding(msg);
	contents.add(msg);
	clusterSize++;
    }

    public List<Vector> getContentVecs() {
	List<Message> messages = new ArrayList<>();
	for (ClusterableObject obj : contents) {
	    messages.add(((ClusterableMessage) obj).getMessage());
	}
	try {
	    return Clusterer.getVectoriser().doc2vec(messages);
	} catch (BatchSizeTooSmallException e) {
	    return null;
	}

    }

    // updateServer() method? Could take mailbox as argument and use Mailbox to
    // update server. Update could also be in
    // addMessage()
}
