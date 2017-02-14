package uk.ac.cam.cl.charlie.vec;

import java.util.Arrays;

/**
 * Created by shyam on 06/02/2017.
 */
public class TextVector {
    private final double[] components;

    public TextVector(double[] data) {
	components = data.clone(); // immutability
    }

    private TextVector(double[] data, boolean dummy) {
	components = data;
    }

    public double[] getRawComponents() {
	return components.clone(); // encapsulation
    }

    public double get(int index) {
	return components[index];
    }

    // public void set(int index, double component) {components[index] =
    // component;}
    public int size() {
	return components.length;
    }

    public TextVector normalize() {
	double dotProduct = 0;
	for (double d : components) {
	    dotProduct += d * d;
	}
	double magnitude = Math.sqrt(dotProduct);
	double[] comp = new double[components.length];
	for (int i = 0; i < components.length; i++) {
	    comp[i] = components[i] / magnitude;
	}
	return new TextVector(comp, false);
    }

    @Override
    public String toString() {
	return "TextVector{" + "components=" + Arrays.toString(components) + '}';
    }
}