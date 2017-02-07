package uk.ac.cam.cl.charlie.clustering;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.clusterers.SimpleKMeans;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.PrincipalComponents;

import javax.mail.Message;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Probably going to be the final clusterer. Or could try density based clustering.
 */
public class EMClusterer extends Clusterer{
    private final String DEFAULT_ARFF = "vectors.arff";

    //Note: currently set up to train on every vector. If this is uses too much memory, could train on a subset.
    protected ArrayList<Cluster> run(ArrayList<Message> messages) throws Exception{

        //Not yet implemented vectoriser so use DummyVectoriser for testing:
        ArrayList<Vector<Double>> vecs = new ArrayList<Vector<Double>>();

        for (Message m : messages)
           vecs.add(DummyVectoriser.vectorise(m));

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
            ArrayList<ArrayList<Message>> msgGroups = new ArrayList<ArrayList<Message>>();
            for (int i = 0; i < cl.numberOfClusters(); i++)
                msgGroups.add(new ArrayList<Message>());

            //For each message, get the corresponding Instance object, and find what cluster it belongs to.
            //Then add it to the corresponding message grouping.
            for (int i = 0; i < messages.size(); i++) {
                Instance curr = data.get(i);
                int clusterIndex = cl.clusterInstance(curr);
                msgGroups.get(clusterIndex).add(messages.get(i));
            }

            //Create new EMCluster objects to contain the message groupings.
            ArrayList<Cluster> clusters = new ArrayList<Cluster>();
            for (int i = 0; i < cl.numberOfClusters(); i++)
                clusters.add(new EMCluster(msgGroups.get(i)));

            return clusters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void classifyNewEmails(ArrayList<Message> messages) throws VectorElementMismatchException {
        ArrayList<Cluster> clusters = getClusters();

        //For classification, find clustering with highest probability for each email using matchStrength().
        //TODO: Update mailbox accordingly. Could be a method in Clusterer itself.

        //For each new message,
        for (int i = 0; i < messages.size(); i++) {
            double bestMatch = Integer.MAX_VALUE;
            int bestCluster = 0;
            //Find the index of the best cluster,
            for (int j = 0; j < clusters.size(); j++) {
                double currMatch = clusters.get(j).matchStrength(messages.get(i));
                if (currMatch > bestMatch) {
                    bestMatch = currMatch;
                    bestCluster = j;
                }
            }
            //and insert the message into that cluster.
            clusters.get(bestCluster).addMessage(messages.get(i));
        }
    }

    void createArff(ArrayList<Vector<Double>> vecs, String fileName) throws IOException {
        FileWriter writer = new FileWriter(fileName);
        int dimensionality = vecs.get(0).size();

        //header with names for each attribute e0,...,en
        writer.write("@RELATION vectors \n");
        for (int i = 0; i < dimensionality; i++) {
            writer.write("@ATTRIBUTE e" + i + " REAL \n");
        }
        writer.write("\n@DATA\n");

        //Print a line for each vector, with elements separated by a comma.
        for (Vector<Double> v : vecs) {
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

    //Note: this function may not even be needed.
    ArrayList<Vector<Double>> parseArff(String fileName) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(fileName));
        String currLine;
        //Note: Only numbers can be used as attributes. No strings.
        //Although, could use strings if we consider sentiment analysis for example.

        ArrayList<Vector<Double>> vecs = new ArrayList<Vector<Double>>();
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
                    Vector<Double> v = new Vector<Double>();
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
}
