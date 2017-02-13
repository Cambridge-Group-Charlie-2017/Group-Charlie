package uk.ac.cam.cl.charlie.clustering;

import uk.ac.cam.cl.charlie.vec.TextVector;


/**
 * @author M Boyce
 */
public interface ClusterableObject {

    /*
    * Return the current vector representation of this message.
    * If the vectoriser cannot return it for any reason, null is returned.
    */
    public TextVector getVec();
}
