package uk.ac.cam.cl.charlie.clustering.clusterableObjects;

import javax.mail.Message;

import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.BatchSizeTooSmallException;

/**
 * Created by M Boyce on 11/02/2017.
 */
public class ClusterableMessage implements ClusterableObject {
    private Message message;

    public ClusterableMessage(Message msg) {
	message = msg;
    }

    @Override
    public Vector getVec() {
	try {
	    return Clusterer.getVectoriser().doc2vec(message);
	} catch (BatchSizeTooSmallException e) {
	    return null;
	}
    }

    @Override
    public boolean equals(Object message2) {
	return message.equals(((ClusterableMessage) message2).getMessage());
    }

    public Message getMessage() {
	return message;
    }
}
