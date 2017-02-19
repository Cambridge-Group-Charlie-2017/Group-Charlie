package uk.ac.cam.cl.charlie.clustering.clusters;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.IncompatibleDimensionalityException;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Ben on 07/02/2017.
 */

/*
 * ClusterGroup is a grouping of the current clusters, representing the part of the IMAP folder structure involved in
 * the clustering process. (i.e. no clusters exist representing protected folders)
 * This is fundamentally a wrapper for ArrayList<Cluster> clusters, which integrates useful methods for manipulating
 * and querying the clusters.
 */
public class ClusterGroup implements Iterable<Cluster>{
    protected ArrayList<Cluster> clusters;
    protected int dimensionality;

    public int getDimensionality() {return dimensionality;}

    public ClusterGroup() {
        clusters = new ArrayList<Cluster>();
        dimensionality = 0;
    }

    public int size() {return clusters.size();}

    public Cluster get(int i) {return clusters.get(i);}

    public void add(Cluster c) throws IncompatibleDimensionalityException {
        if (c.getDimensionality() != dimensionality) {
            if (dimensionality == 0)
                dimensionality = c.getDimensionality();
            else throw new IncompatibleDimensionalityException();
        }
        if (c == null)
            return;

        clusters.add(c);
    }

    //returns index of cluster containing m. return -1 if not contained.
    public int queryCluster(ClusterableObject co) {
        for (int i = 0; i < clusters.size(); i++)
            if (clusters.get(i).contains(co))
                return i;

        //could throw exception instead.
        return -1;
    }

    //int insert(Message) to insert a message into the best cluster. Returns index of cluster it inserted into.
    public int insert(ClusterableObject co) throws IncompatibleDimensionalityException{
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
        clusters.get(bestCluster).addMessage(co);
        return bestCluster;
    }

    public Iterator<Cluster> iterator() {
        return clusters.iterator();
    }
}
