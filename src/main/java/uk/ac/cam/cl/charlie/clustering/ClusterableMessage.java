package uk.ac.cam.cl.charlie.clustering;

import uk.ac.cam.cl.charlie.vec.TextVector;
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
        return GenericDummyVectoriser.vectorise(message);
    }

    public Message getMessage() {
        return message;
    }
}
