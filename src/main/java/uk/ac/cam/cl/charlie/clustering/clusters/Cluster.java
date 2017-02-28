package uk.ac.cam.cl.charlie.clustering.clusters;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.clustering.IncompatibleDimensionalityException;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableWordAndOccurence;
import uk.ac.cam.cl.charlie.math.Vector;

public abstract class Cluster {

    protected String clusterName;
    protected ArrayList<ClusterableObject> contents;
    private int dimensionality;
    private int clusterSize;

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

    /**
     * Abstract method used for testing which cluster is the best match for a
     * specific Message. Actual implementation varies between implementations.
     * For EMCluster, the output is proportional to the Naive Bayes probability
     * of a match, so higher values are better.
     *
     * @param msg
     * @return
     * @throws IncompatibleDimensionalityException
     */
    abstract double matchStrength(ClusterableObject msg) throws IncompatibleDimensionalityException;

    //returns whether high, or low, match strength value is preferable.
    public abstract boolean isHighMatchGood();

    // Extract relevant metadata from the initial contents.
    protected Cluster(ArrayList<ClusterableObject> initialContents) {
    	contents = initialContents;
	    clusterSize = initialContents.size();
	    dimensionality = initialContents.get(0).getVector().size();
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
        List<Vector> vectors = new ArrayList<>();
        for (ClusterableObject obj : contents) {
            if(obj instanceof ClusterableMessage)
                messages.add(((ClusterableMessage) obj).getMessage());
            else if(obj instanceof ClusterableWordAndOccurence)
                vectors.add(((ClusterableWordAndOccurence)obj).getVec());
        }

        vectors.addAll(Clusterer.getVectoriser().doc2vec(messages));
        return vectors;
    }
}
