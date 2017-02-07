package uk.ac.cam.cl.charlie.vec;

/**
 * Created by shyam on 06/02/2017.
 */
public class TextVector {
    private final double[] components;

    public TextVector(double[] data) {
        components = data.clone(); // immutability
    }

    public double[] getRawComponents() {
        return components.clone(); // encapsulation
    }
}