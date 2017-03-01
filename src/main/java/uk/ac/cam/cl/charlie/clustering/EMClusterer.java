package uk.ac.cam.cl.charlie.clustering;

import java.util.ArrayList;
import java.util.List;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;
import uk.ac.cam.cl.charlie.clustering.clusters.EMCluster;
import uk.ac.cam.cl.charlie.math.Vector;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.PrincipalComponents;

/**
 * Probably going to be the final clusterer. Or could try density based
 * clustering.
 */
public class EMClusterer<T> extends Clusterer<T> {
    //
    public EMClusterer(List<? extends ClusterableObject<T>> objects) {
        evalClusters(objects);
    }

    // Note: currently set up to train on every vector. If this is uses too much
    // memory, could train on a subset.
    // @Override
    @Override
    protected ClusterGroup<T> run(List<? extends ClusterableObject<T>> objects) throws Exception {
        // Assume dimensions are all the same
        int dimension = objects.get(0).getVector().size();

        // Create attributes
        ArrayList<Attribute> attributes = new ArrayList<>();
        for (int i = 0; i < dimension; i++) {
            attributes.add(new Attribute("e" + i));
        }

        // Fill data into instances
        Instances data = new Instances("vectors", attributes, objects.size());

        for (ClusterableObject<T> obj : objects) {
            Instance instance = new DenseInstance(dimension);
            Vector vec = obj.getVector();
            for (int i = 0; i < dimension; i++) {
                instance.setValue(attributes.get(i), vec.get(i));
            }
            data.add(instance);
        }

        EM cl;
        PrincipalComponents pca;
        try {
            // Options: max 5 iterations. 5 clusters.
            cl = new EM();

            // dimensionality reduction here.
            // First use PCA, then use random projection to get to the desired
            // dimensionality.
            // possible option: -R <num> for proportion of variance to maintain.
            // Default 0.95, could go lower.
            pca = new PrincipalComponents();
            pca.setInputFormat(data);
            pca.setMaximumAttributes(20);

            // apply filter
            data = Filter.useFilter(data, pca);
            // If efficiency is a problem, could use random projection instead.

            // Initially cluster with no cluster number preference.
            String[] options = { "-I", "5" };
            cl.setOptions(options);
            cl.buildClusterer(data);
            ClusterEvaluation eval = new ClusterEvaluation();
            eval.setClusterer(cl);
            eval.evaluateClusterer(new Instances(data));
            // If not clustered properly, try again with n=5 to ensure a
            // grouping is found.
            if (cl.numberOfClusters() <= 0) {
                String[] options2 = { "-I", "5", "-N", "5" };
                cl.setOptions(options2);
                cl.buildClusterer(data);
                eval = new ClusterEvaluation();
                eval.setClusterer(cl);
                eval.evaluateClusterer(new Instances(data));
            }

            // Create message groupings for each cluster.
            ArrayList<ArrayList<ClusterableObject<T>>> objGroups = new ArrayList<>();
            for (int i = 0; i < cl.numberOfClusters(); i++)
                objGroups.add(new ArrayList<ClusterableObject<T>>());

            // For each message, get the corresponding Instance object, and find
            // what cluster it belongs to.
            // Then add it to the corresponding message grouping.
            for (int i = 0; i < objects.size(); i++) {
                Instance curr = data.get(i);
                int clusterIndex = cl.clusterInstance(curr);
                objGroups.get(clusterIndex).add(objects.get(i));
            }

            // Wrap the clusters in a ClusterGroup object.
            ClusterGroup<T> clusters = new ClusterGroup<>();
            for (int i = 0; i < cl.numberOfClusters(); i++)
                clusters.add(new EMCluster<>(objGroups.get(i)));

            return clusters;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
