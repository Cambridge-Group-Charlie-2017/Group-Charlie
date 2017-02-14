package uk.ac.cam.cl.charlie.clustering;

/**
 * Created by Ben on 11/02/2017.
 */
public class DemoMessageVector {
    public ClusterableObject message;
    public double[] vec;
    public String nameOfCluster;

    public DemoMessageVector(double[] arr, ClusterableObject msg) {
	message = msg;
	vec = arr.clone();
    }
}
