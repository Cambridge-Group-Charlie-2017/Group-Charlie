package uk.ac.cam.cl.charlie.math;

import java.util.Arrays;

public class Vector {
    private final double[] components;

    public Vector(double[] data) {
	components = data.clone();
    }

    private Vector(double[] data, boolean dummy) {
	components = data;
    }

    public double[] toDoubleArray() {
	return components.clone();
    }

    public double get(int index) {
	return components[index];
    }

    public int size() {
	return components.length;
    }

    public double dot(Vector vec) {
	if (components.length != vec.components.length) {
	    throw new IllegalArgumentException("Dimension must match to calculate dot product");
	}
	double dotProduct = 0;
	for (int i = 0; i < components.length; i++) {
	    dotProduct += components[i] * vec.components[i];
	}
	return dotProduct;
    }

    public double dotSquare() {
	double dotProduct = 0;
	for (double d : components) {
	    dotProduct += d * d;
	}
	return dotProduct;
    }

    public double magnitude() {
	return Math.sqrt(dotSquare());
    }

    public Vector scale(double v) {
	double[] comp = new double[components.length];
	for (int i = 0; i < components.length; i++) {
	    comp[i] = components[i] * v;
	}
	return new Vector(comp, false);
    }

    public Vector normalize() {
	return scale(1 / magnitude());
    }

    @Override
    public String toString() {
	return "Vector(" + Arrays.toString(components) + ")";
    }
}