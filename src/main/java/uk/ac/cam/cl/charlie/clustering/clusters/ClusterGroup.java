package uk.ac.cam.cl.charlie.clustering.clusters;

import java.util.ArrayList;
import java.util.Iterator;

import uk.ac.cam.cl.charlie.clustering.IncompatibleDimensionalityException;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;

/**
 * Created by Ben on 07/02/2017.
 */

/*
 * ClusterGroup is a grouping of the current clusters, representing the part of
 * the IMAP folder structure involved in the clustering process. (i.e. no
 * clusters exist representing protected folders) This is fundamentally a
 * wrapper for ArrayList<Cluster> clusters, which integrates useful methods for
 * manipulating and querying the clusters.
 */
public class ClusterGroup<T> implements Iterable<Cluster<T>> {
    protected ArrayList<Cluster<T>> clusters;
    protected int dimension;

    public int getDimensionality() {
        return dimension;
    }

    public ClusterGroup() {
        clusters = new ArrayList<>();
        dimension = 0;
    }

    public int size() {
        return clusters.size();
    }

    public Cluster<T> get(int i) {
        return clusters.get(i);
    }

    public Cluster<T> get(String name) {
        for (Cluster c : clusters)
            if (c.getName().equals("name"))
                return c;
        return null;
    }

    public void add(Cluster<T> c) throws IncompatibleDimensionalityException {
        if (c.getDimension() != dimension) {
            if (dimension == 0)
                dimension = c.getDimension();
            else
                throw new IncompatibleDimensionalityException();
        }

        clusters.add(c);
    }

    // returns index of cluster containing m. return -1 if not contained.
    public int queryCluster(ClusterableObject<T> co) {
        for (int i = 0; i < clusters.size(); i++)
            if (clusters.get(i).contains(co))
                return i;

        // could throw exception instead.
        return -1;
    }

    // String insert(Message) to insert a message into the best cluster. Returns
    // name of cluster it inserted into.
    public String insert(ClusterableObject<T> co) throws IncompatibleDimensionalityException {
        int bestCluster = 0;
        boolean prioritiseHigh = clusters.get(0).isHighMatchGood();

        double bestMatch = prioritiseHigh ? Double.MIN_VALUE : Double.MAX_VALUE;
        for (int i = 0; i < clusters.size(); i++) {
            double currMatch = clusters.get(i).matchStrength(co);
            if (currMatch > bestMatch && prioritiseHigh || currMatch < bestMatch && !prioritiseHigh) {
                bestMatch = currMatch;
                bestCluster = i;
            }
        }
        Cluster best = clusters.get(bestCluster);
        best.addObject(co);
        return best.getName();
    }

    @Override
    public Iterator<Cluster<T>> iterator() {
        return clusters.iterator();
    }
}
