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
public class EMClusterer extends Clusterer {
    private final String DEFAULT_ARFF = "vectors.arff";

    // Note: currently set up to train on every vector. If this is uses too much
    // memory, could train on a subset.
    @Override
    public ClusterGroup run(ClusterableObjectGroup objects) throws Exception {

		List<Vector> vecs = objects.getVecs();

		int dimension = vecs.get(0).size();

		// Create attributes
		ArrayList<Attribute> attributes = new ArrayList<>();
		for (int i = 0; i < dimension; i++) {
			attributes.add(new Attribute("e" + i));
		}

		Instances data = new Instances("vectors", attributes, vecs.size());

		for (Vector vec : vecs) {
			Instance instance = new DenseInstance(dimension);
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

			String[] options = { "-I", "5" };
			cl.setOptions(options);

			// TODO: could initially not set a cluster number, rebuild with n=5
			// if output has a useless number of clusters.
			cl.buildClusterer(data);
			ClusterEvaluation eval = new ClusterEvaluation();
			eval.setClusterer(cl);
			eval.evaluateClusterer(new Instances(data));

			// Create message groupings for each cluster.
			ArrayList<ArrayList<ClusterableObject>> objGroups = new ArrayList<>();
			for (int i = 0; i < cl.numberOfClusters(); i++)
			objGroups.add(new ArrayList<ClusterableObject>());

			// For each message, get the corresponding Instance object, and find
			// what cluster it belongs to.
			// Then add it to the corresponding message grouping.
			for (int i = 0; i < objects.size(); i++) {
			Instance curr = data.get(i);
			int clusterIndex = cl.clusterInstance(curr);
			objGroups.get(clusterIndex).add(objects.get(i));
			}

			// Create new Cluster objects in a ClusterGroup to contain the
			// message groupings.
			ClusterGroup clusters = new ClusterGroup();
			for (int i = 0; i < cl.numberOfClusters(); i++)
			clusters.add(new EMCluster(objGroups.get(i)));

			return clusters;

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
    }
}
