package uk.ac.cam.cl.charlie.clustering;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.SimpleKMeans;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

import javax.mail.Message;

/**
 * Created by Ben on 01/02/2017.
 */

// Using KMeans initially purely for test reasons, because it's easy.
// Will definitely try other algorithms once the basic structure works.
// Using a default 5 clusters.

public class KMeansClusterer extends Clusterer {
    //File in which vectors will be stored temporarily prior to clustering.
    private final String DEFAULT_ARFF = "vectors.arff";

    protected ArrayList<Cluster> run(ArrayList<Message> messages) throws Exception{

        //Not yet implemented so we'll use a substitute for testing:
        vecsForTesting = parseTestFile();

        // convert vecs into aarf format, invoke KMeans with default k=5.
        createArff(vecsForTesting, DEFAULT_ARFF);

        SimpleKMeans cl;
        try {
            //Options: max 5 iterations. 5 clusters.
            String[] options = {"-I", "5", "-N", "5"};
            cl = new SimpleKMeans();
            Instances data = ConverterUtils.DataSource.read(DEFAULT_ARFF);
            cl.setOptions(options);
            cl.buildClusterer(data);
            ClusterEvaluation eval = new ClusterEvaluation();
            eval.setClusterer(cl);
            //eval.evaluateClusterer(new Instances(data));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        int dimensionality = vecsForTesting.get(0).size();
        Instances clusterCentroids = cl.getClusterCentroids();


        ArrayList<Vector<Double>> centroids = new ArrayList<Vector<Double>>();
        ArrayList<ArrayList<Message>> msgGroups = new ArrayList<ArrayList<Message>>();

        //create centroid vectors from the clustering results.
        for (int i = 0; i < cl.numberOfClusters(); i++) {
            Vector<Double> centVec = new Vector<Double>();
            for (int j = 0; j < dimensionality; j++) {
                centVec.add(clusterCentroids.attributeToDoubleArray(j)[i]);
            }
            centroids.add(centVec);
        }


        //group messages into msgGroups. Place each in the list at the same index as their closest centroid.
        //Again, use test vectors until Vectoriser is implemented.
        double bestMatch = Integer.MAX_VALUE;
        int bestCluster = 0;
        for (int i = 0; i < messages.size(); i++) {
            for (int j = 0; j < clusters.size(); j++) {
                double currMatch = distanceSquared(vecsForTesting.get(i), centroids.get(i));
                if (currMatch < bestMatch) {
                    bestMatch = currMatch;
                    bestCluster = j;
                }
            }
            msgGroups.get(bestCluster).add(messages.get(i));
        }

        //create uk.ac.cam.cl.charlie.clustering.Cluster objects using these groupings
        clusters = new ArrayList<Cluster>();
        for (int i = 0; i < centroids.size(); i++) {
            clusters.add(new KMeansCluster(centroids.get(i), msgGroups.get(i)));
        }

        //return current clustering to calling class (probably Client). Could be ignored.
        return clusters;
    }

    public double distanceSquared(Vector<Double> vec1, Vector<Double> vec2) throws VectorElementMismatchException {
        if (vec1.size() != vec2.size())
            throw new VectorElementMismatchException();

        //returns square of distance
        double distanceSquared = 0;
        for (int i = 0; i < vec2.size(); i++) {
            distanceSquared += Math.pow(vec2.get(i) - vec1.get(i), 2.0);
        }
        return distanceSquared;
    }

    void createArff(ArrayList<Vector<Double>> vecs, String fileName) throws IOException {
        FileWriter writer = new FileWriter(fileName);
        int dimensionality = vecs.get(0).size();

        writer.write("@RELATION vectors \n");
        for (int i = 0; i < dimensionality; i++) {
            writer.write("@ATTRIBUTE e" + i + " REAL \n");
        }
        writer.write("\n@DATA\n");

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

    //temporary function for testing. Will be made redundant once vectoriser is implemented.
    private ArrayList<Vector<Double>> parseTestFile() {
        try {
            return parseArff("iris-vector.arff");
        } catch (IOException e) {return null;}
    }
}
