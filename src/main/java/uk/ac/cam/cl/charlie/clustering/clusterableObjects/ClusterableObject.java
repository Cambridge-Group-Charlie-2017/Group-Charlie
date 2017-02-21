package uk.ac.cam.cl.charlie.clustering.clusterableObjects;

import uk.ac.cam.cl.charlie.math.Vector;

/**
 * @author M Boyce
 */
public abstract class ClusterableObject {

    Vector vector;

    /*
     * Return the current vector representation of this message. If the
     * vectoriser cannot return it for any reason, null is returned.
     */
    protected abstract Vector getVec();

    public Vector getVector() {
        if (vector == null) {
            vector = getVec();
        }
        return vector;
    }
}
