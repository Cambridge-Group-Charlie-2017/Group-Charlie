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

    public Vector add(Vector vec) {
        if (components.length != vec.components.length) {
            throw new IllegalArgumentException("Dimension must match to calculate dot product");
        }

        double[] comp = new double[components.length];
        for (int i = 0; i < components.length; i++) {
            comp[i] = components[i] + vec.components[i];
        }
        return new Vector(comp, false);
    }

    public Vector normalize() {
        double magnitude = magnitude();
        if (magnitude == 0)
            return this;
        return scale(1 / magnitude);
    }

    @Override
    public String toString() {
        return "Vector(" + Arrays.toString(components) + ")";
    }

    public static Vector zero(int dim) {
        double[] comp = new double[dim];
        return new Vector(comp, false);
    }

    public static Vector concat(Vector head, Vector tail) {
        int headlength = head.components.length;
        int taillength = tail.components.length;
        double[] newVector = new double[headlength + taillength];
        System.arraycopy(head.components, 0, newVector, 0, headlength);
        System.arraycopy(tail.components, 0, newVector, headlength, taillength);
        return new Vector(newVector, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Vector vector = (Vector) o;

        return Arrays.equals(components, vector.components);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(components);
    }

    public static Vector weightedAverage(Vector addition, Vector main, double weight) {
        double[] average = main.components;
        double[] add = addition.components;
        for (int i = 0; i < average.length; ++i) {
            average[i] = average[i] + weight * add[i];
        }
        return new Vector(average, false);
    }
}
