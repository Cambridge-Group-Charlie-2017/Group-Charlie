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

/**
 * Created by Ben on 01/02/2017.
 */

// Using KMeans initially purely for test reasons, because it's easy.
// Will definitely try other algorithms once the basic structure works.

public class KMeansWrapper extends ClusteringAlgorithmWrapper{
    private final String DEFAULT_ARFF = "iris-vector.arff";

    public ArrayList<Cluster> run(ArrayList<Vector<Double>> vecs) throws Exception{
        // convert vecs into aarf format, invoke KMeans with default k=5.
        try {
            createArff(vecs, DEFAULT_ARFF);
        } catch (IOException e) {
            //TODO: deal with
        }

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
            eval.evaluateClusterer(new Instances(data));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        int dimensionality = vecs.get(0).size();
        //Instances clusterStandardDevs = cl.getClusterStandardDevs();
        Instances clusterCentroids = cl.getClusterCentroids();

        for (int i = 0; i < cl.numberOfClusters(); i++) {
            Vector<Double> centVec = new Vector<Double>();
            for (int j = 0; j < dimensionality; j++) {
                centVec.add(clusterCentroids.attributeToDoubleArray(j)[i]);
            }
            clusters.add(new KMeansCluster(centVec));
        }

        return clusters;
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
}
