package uk.ac.cam.cl.charlie.clustering.clusters;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.cam.cl.charlie.clustering.IncompatibleDimensionalityException;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.math.Vector;

public abstract class Cluster<T> {

    protected String name;
    protected ArrayList<ClusterableObject<T>> objects;
    private int dimension;
    private boolean nameConfidence;

    public int getDimension() {
        return dimension;
    }

    public int getSize() {
        return objects.size();
    }

    public ArrayList<ClusterableObject<T>> getObjects() {
        return objects;
    }

    public String getName() {
        return name;
    }

    // Naming is a separate process to clustering, so the name can be assigned
    // later.
    public void setName(String name) {
        this.name = name;
    }

    public boolean contains(ClusterableObject<T> obj) {
        return objects.contains(obj);
    }

    /*
     * Abstract method used for testing which cluster is the best match for a
     * specific Message. Actual implementation varies between implementations.
     * For EMCluster, the output is proportional to the Naive Bayes probability
     * of a match, so higher values are better.
     */
    abstract double matchStrength(ClusterableObject<T> obj) throws IncompatibleDimensionalityException;

    public abstract boolean isHighMatchGood();

    // Extract relevant metadata from the initial contents.
    protected Cluster(ArrayList<ClusterableObject<T>> initialObjects) {
        objects = new ArrayList<>(initialObjects);
        dimension = initialObjects.get(0).getVector().size();
    }

    protected abstract void updateMetadataAfterAdding(ClusterableObject<T> obj);

    // adding a new message to a clustering (during classification) should cause
    // clustering metadata to change accordingly.
    public void addObject(ClusterableObject<T> obj) {
        updateMetadataAfterAdding(obj);
        objects.add(obj);
    }

    public List<Vector> getVectors() {
        return objects.stream().map(obj -> obj.getVector()).collect(Collectors.toList());
    }

    public boolean getNameConfidence() {
        return nameConfidence;
    }

    public void setNameConfidence(boolean nameConfidence) {
        this.nameConfidence = nameConfidence;
    }
}
