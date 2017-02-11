package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Message;

/**
 * Created by Ben on 11/02/2017.
 */
public class DemoMessageVector {
    public ClusterableObject message;
    public double[] vec;
    public DemoMessageVector(double[] arr, ClusterableObject msg) {
        message = msg;
        vec = arr.clone();
    }
}
