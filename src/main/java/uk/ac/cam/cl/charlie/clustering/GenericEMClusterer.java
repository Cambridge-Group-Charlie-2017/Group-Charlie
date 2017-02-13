package uk.ac.cam.cl.charlie.clustering;

import uk.ac.cam.cl.charlie.vec.TextVector;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.core.pmml.jaxbbindings.TextCorpus;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.PrincipalComponents;

import javax.mail.Message;
import javax.xml.soap.Text;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Probably going to be the final clusterer. Or could try density based clustering.
 */
public class GenericEMClusterer extends GenericClusterer{
    private final String DEFAULT_ARFF = "vectors.arff";

    //Note: currently set up to train on every vector. If this is uses too much memory, could train on a subset.
    public GenericClusterGroup run(ClusterableObjectGroup objects) throws Exception {

        ArrayList<TextVector> vecs = objects.getVecs();

        // convert vecs into arff format for the clusterer.
        // is there a more efficient way of converting to (dense) Instances? add(Instance)
        createArff(vecs, DEFAULT_ARFF);

        EM cl;
        PrincipalComponents pca;
        try {
            //Options: max 5 iterations. 5 clusters.

            cl = new EM();
            Instances data = ConverterUtils.DataSource.read(DEFAULT_ARFF);

            //dimensionality reduction here.
            //First use PCA, then use random projection to get to the desired dimensionality.
            //possible option: -R <num> for proportion of variance to maintain. Default 0.95, could go lower.
            pca = new PrincipalComponents();
            pca.setInputFormat(data);
            pca.setMaximumAttributes(20);

            //apply filter
            data = Filter.useFilter(data, pca);
            //If efficiency is a problem, could use random projection instead.

            String[] options = {"-I", "5"};
            cl.setOptions(options);

            //TODO: could initially not set a cluster number, rebuild with n=5 if output has a useless number of clusters.
            cl.buildClusterer(data);
            ClusterEvaluation eval = new ClusterEvaluation();
            eval.setClusterer(cl);
            eval.evaluateClusterer(new Instances(data));

            //Create message groupings for each cluster.
            ArrayList<ArrayList<ClusterableObject>> objGroups = new ArrayList<ArrayList<ClusterableObject>>();
            for (int i = 0; i < cl.numberOfClusters(); i++)
                objGroups.add(new ArrayList<ClusterableObject>());

            //For each message, get the corresponding Instance object, and find what cluster it belongs to.
            //Then add it to the corresponding message grouping.
            for (int i = 0; i < objects.size(); i++) {
                Instance curr = data.get(i);
                int clusterIndex = cl.clusterInstance(curr);
                objGroups.get(clusterIndex).add(objects.get(i));
            }

            //Create new Cluster objects in a ClusterGroup to contain the message groupings.
            GenericClusterGroup clusters = new GenericClusterGroup();
            for (int i = 0; i < cl.numberOfClusters(); i++)
                clusters.add(new GenericEMCluster(objGroups.get(i)));

            return clusters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    void createArff(ArrayList<TextVector> vecs, String fileName) throws IOException {
        FileWriter writer = new FileWriter(fileName);
        int dimensionality = vecs.get(0).size();

        //header with names for each attribute e0,...,en
        writer.write("@RELATION vectors \n");
        for (int i = 0; i < dimensionality; i++) {
            writer.write("@ATTRIBUTE e" + i + " REAL \n");
        }
        writer.write("\n@DATA\n");

        //Print a line for each vector, with elements separated by a comma.
        for (TextVector v : vecs) {
            String vecString = "";
            for (int i = 0; i < dimensionality; i++) {
                vecString += String.format("%f", v.get(i));
                if (i < dimensionality - 1)
                    vecString += ",";
            }
            writer.write(vecString + "\n");
        }
        writer.flush();
        writer.close();
    }

/*
    //Note: this function may not even be needed.
    ArrayList<TextVector> parseArff(String fileName) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String currLine;
        //Note: Only numbers can be used as attributes. No strings.
        //Although, could use strings if we consider sentiment analysis for example.

        ArrayList<TextVector> vecs = new ArrayList<TextVector>();
        int dimensionality = 0;

        while(true) {
            currLine = in.readLine();
            String[] words = currLine.split("\\s+");
            if (words[0].equals("@ATTRIBUTE"))
                dimensionality++;
            if (words[0].equals("@DATA"))
                break;
        }

        try {
            while (true) {
                currLine = in.readLine();
                if (currLine == null)
                    break;
                if (!currLine.equals("")) {
                    String[] tokens = currLine.split(",");
                    TextVector v = new TextVector();
                    for (int i = 0; i < dimensionality; i++) {
                        double element = Double.parseDouble(tokens[i]);
                        v.add(element);
                    }
                    vecs.add(v);
                }
            }
        } catch (Exception e) {e.printStackTrace();}

        return vecs;
    }
*/

    public ArrayList<ArrayList<DemoMessageVector>> demoClusterer(ArrayList<ClusterableObject> messages) throws Exception{

        //Not yet implemented vectoriser so use DummyVectoriser for testing:
        ArrayList<TextVector> vecs = new ArrayList<TextVector>();

        GenericDummyVectoriser.train(messages);
        for (ClusterableObject m : messages)
            vecs.add(GenericDummyVectoriser.vectorise(m));

        // convert vecs into arff format for the clusterer.
        // is there a more efficient way of converting to (dense) Instances? add(Instance)
        createArff(vecs, DEFAULT_ARFF);

        EM cl;
        PrincipalComponents pca;
        try {
            cl = new EM();
            Instances data = ConverterUtils.DataSource.read(DEFAULT_ARFF);

            //dimensionality reduction here.
            //First use PCA, then use random projection to get to the desired dimensionality.
            //possible option: -R <num> for proportion of variance to maintain. Default 0.95, could go lower.
            pca = new PrincipalComponents();
            pca.setInputFormat(data);
            pca.setMaximumAttributes(2); //allowing a 2D plot

            //apply filter
            data = Filter.useFilter(data, pca);
            //If efficiency is a problem, could use random projection instead.

            String[] options = {"-I", "5"};
            cl.setOptions(options);

            //TODO: could initially not set a cluster number, rebuild with n=5 if output has a useless number of clusters.
            cl.buildClusterer(data);
            ClusterEvaluation eval = new ClusterEvaluation();
            eval.setClusterer(cl);
            eval.evaluateClusterer(new Instances(data));

            ArrayList<ArrayList<DemoMessageVector>> results = new ArrayList<ArrayList<DemoMessageVector>>();
            for (int i = 0; i < cl.numberOfClusters(); i++) {
                results.add(new ArrayList<DemoMessageVector>());
            }
            //For each message, get the corresponding Instance object, and find what cluster it belongs to.
            //Then add it to the corresponding message grouping.
            for (int i = 0; i < messages.size(); i++) {
                Instance curr = data.get(i);
                int clusterIndex = cl.clusterInstance(curr);
                //insert corresponding vector
                results.get(clusterIndex).add(new DemoMessageVector(curr.toDoubleArray(), messages.get(i)));
            }

            return results;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
