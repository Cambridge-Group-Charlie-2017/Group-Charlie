package uk.ac.cam.cl.charlie.clustering.clusterableObjects;

import java.util.Objects;

import uk.ac.cam.cl.charlie.math.Vector;

/**
 * @author M Boyce
 * @author Gary Guo
 */
public class ClusterableObject<T> {

    protected T object;
    protected Vector vec;

    public ClusterableObject(T object, Vector vec) {
        this.object = object;
        this.vec = vec;
    }

    public Vector getVector() {
        return vec;
    }

    public T getObject() {
        return object;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClusterableObject) {
            return Objects.equals(object, ((ClusterableObject<?>) obj).object);
        }
        return false;
    }
}
