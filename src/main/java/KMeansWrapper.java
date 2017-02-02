import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import weka.clusterers.*;
import weka.core.Instances;
import weka.core.converters.ConverterUtils;

/**
 * Created by Ben on 01/02/2017.
 */

// Using KMeans initially purely for test reasons, because it's easy.
// Will definitely try other algorithms once the basic structure works.

public class KMeansWrapper extends ClusteringAlgorithmWrapper{


    public ArrayList<Cluster> run(ArrayList<Vector<Double>> vecs) throws Exception{
        //TODO: convert vecs into aarf format, invoke KMeans with default k=5.
        try {
            createAarf(vecs, "tempFile.aarf");
        } catch (IOException e) {
            //TODO: deal with
        }

        SimpleKMeans cl;
        try {
            //Options: max 5 iterations. 5 clusters.
            String[] options = {"-I", "5", "-N", "5"};
            cl = new SimpleKMeans();
            Instances data = ConverterUtils.DataSource.read("tempFile.aarf");
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
        Instances clusterStandardDevs = cl.getClusterStandardDevs();
        Instances clusterCentroids = cl.getClusterCentroids();

        for (int i = 0; i < cl.numberOfClusters(); i++) {
            double[] stdevs = clusterStandardDevs.attributeToDoubleArray(i);
            double[] cents = clusterCentroids.attributeToDoubleArray(i);
            Vector<Double> stdevVec = new Vector<Double>();
            Vector<Double> centVec = new Vector<Double>();
            for (int j = 0; j < dimensionality; j++) {
                stdevVec.add(stdevs[j]);
                centVec.add(cents[j]);
            }
            clusters.add(new Cluster(centVec, stdevVec));
        }

        return clusters;
    }

    void createAarf(ArrayList<Vector<Double>> vecs, String fileName) throws IOException {
        FileWriter writer = new FileWriter(fileName);
        int dimensionality = vecs.get(0).size();

        writer.write("@RELATION vectors \n");
        for (int i = 0; i < dimensionality; i++) {
            writer.write("@ATTRIBUTE e" + i + " \n");
        }
        writer.write("\n@DATA\n");

        for (Vector<Double> v : vecs) {
            String vecString = "";
            for (int i = 0; i < dimensionality; i++) {
                vecString += String.format("%f", v.get(i));
                if (i < dimensionality - 1)
                    vecString += ", ";
            }
            writer.write(vecString + "\n");
        }
        writer.flush();
        writer.close();

    }
}
