package uk.ac.cam.cl.charlie.clustering.clusterableObjects;

import javax.mail.Message;

import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.math.Vector;

/**
 * Created by M Boyce on 11/02/2017.
 */
public class ClusterableMessage extends ClusterableObject {
    private Message message;

    public ClusterableMessage(Message msg) {
        message = msg;
    }

    @Override
    public Vector getVec() {
        return Clusterer.getVectoriser().doc2vec(message);
    }

    @Override
    public boolean equals(Object message2) {
        return message.equals(((ClusterableMessage) message2).getMessage());
    }

    public Message getMessage() {
        return message;
    }
}
