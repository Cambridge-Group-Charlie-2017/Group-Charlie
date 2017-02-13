package uk.ac.cam.cl.charlie.clustering;

import uk.ac.cam.cl.charlie.vec.BatchSizeTooSmallException;
import uk.ac.cam.cl.charlie.vec.TextVector;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;

import javax.mail.Message;

/**
 * Created by M Boyce on 11/02/2017.
 */
public class ClusterableMessage implements ClusterableObject {
    private Message message;

    public ClusterableMessage(Message msg){
        message = msg;
    }

    @Override
    public TextVector getVec() {
        try {
            return GenericClusterer.getVectoriser().doc2vec(message);
        } catch (BatchSizeTooSmallException e) {
            return null;
        }
    }

    @Override
    public boolean equals(Object message2) {
        return message.equals(((ClusterableMessage)message2).getMessage());
    }

    public Message getMessage() {
        return message;
    }
}
