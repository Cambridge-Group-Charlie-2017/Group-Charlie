package uk.ac.cam.cl.charlie.clustering;

import weka.clusterers.ClusterEvaluation;
import weka.clusterers.EM;
import weka.clusterers.SimpleKMeans;
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

        //Not yet implemented vectoriser so we'll use a substitute for testing:
        vecsForTesting = getTestVecs(messages.size());

        // convert vecs into arff format for the clusterer.
        //TODO: is there a more efficient way of converting to (dense) Instances?
        createArff(vecsForTesting, DEFAULT_ARFF);

        EM cl;
        PrincipalComponents pca;
        try {
            //Options: max 5 iterations. 5 clusters.
            String[] options = {"-I", "5", "-N", "5"};
            cl = new EM();

            Instances data = ConverterUtils.DataSource.read(DEFAULT_ARFF);

            //dimensionality reduction here.
            //First use PCA, then use random projection to get to the desired dimensionality.
            //possible option: -R <num> for proportion of variance to maintain. Default 0.95, could go lower.
            pca = new PrincipalComponents();
            pca.setInputFormat(data);
            pca.setMaximumAttributes(20);

            //Should it be runFilter()?
            data = Filter.useFilter(data, pca);
            //Can follow the PCA with random projection, aim for about 15D or lower.


            cl.setOptions(options);
            //TODO: could initially not set a cluster number, rebuild only if output has a useless number of clusters.
            cl.buildClusterer(data);
            ClusterEvaluation eval = new ClusterEvaluation();
            eval.setClusterer(cl);
            eval.evaluateClusterer(new Instances(data));

            ArrayList<ArrayList<Message>> msgGroups = new ArrayList<ArrayList<Message>>();
            for (int i = 0; i < cl.numberOfClusters(); i++)
                msgGroups.add(new ArrayList<Message>());

            for (int i = 0; i < messages.size(); i++) {
                //use data.get(index) to obtain the reduced Instance for message at element 'index'.
                //call clusterInstance(Instance instance) to get the index of the cluster. Insert it into that cluster.
                Instance curr = data.get(i);
                int clusterIndex = cl.clusterInstance(curr);
                msgGroups.get(clusterIndex).add(messages.get(i));
            }

            ArrayList<Cluster> clusters = new ArrayList<Cluster>();
            for (int i = 0; i < cl.numberOfClusters(); i++) {
                clusters.add(new EMCluster(msgGroups.get(i)));
            }

            return clusters;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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


    //temp testing function, use until vectoriser is implemented.
    public ArrayList<Vector<Double>> getTestVecs(int num) {
        ArrayList<Vector<Double>> vecs = new ArrayList<Vector<Double>>();

        for (int i = 0; i < num; i++) {
            Vector<Double> vec = new Vector<Double>();
            for (int j = 0; j < 300; j++) {
                vec.add(Math.random() * 10);
            }
            vecs.add(vec);
        }
        return vecs;
    }
}
