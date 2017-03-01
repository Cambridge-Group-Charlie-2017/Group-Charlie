package uk.ac.cam.cl.charlie.clustering.clusterableObjects;

import javax.mail.Message;

import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.math.Vector;

/**
 * @author M Boyce
 * @author Gary Guo
 */
public class ClusterableMessage extends ClusterableObject<Message> {

    public ClusterableMessage(Message msg) {
        super(msg, null);
    }

    @Override
    public Vector getVector() {
        if (vec == null) {
            vec = Clusterer.getVectoriser().doc2vec(object);
        }
        return vec;
    }

}
