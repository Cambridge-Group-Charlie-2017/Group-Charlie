package uk.ac.cam.cl.charlie.clustering;

import javax.mail.Message;
import java.util.Vector;

/**
 * Created by M Boyce on 11/02/2017.
 */
public class ClusterableMessage implements ClusterableObject {
    private Message message;

    public ClusterableMessage(Message msg){
        message = msg;
    }

    @Override
    public Vector<Double> getVec() {
        return DummyVectoriser.vectorise(message);
    }

    public Message getMessage() {
        return message;
    }
}
