package uk.ac.cam.cl.charlie.clustering;

import uk.ac.cam.cl.charlie.vec.BatchSizeTooSmallException;
import uk.ac.cam.cl.charlie.vec.TextVector;

import javax.mail.Message;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Ben on 13/02/2017.
 */
public class ClusterableMessageGroup extends ClusterableObjectGroup {

    protected ArrayList<ClusterableMessage> contents;

    public ClusterableMessageGroup(ArrayList<ClusterableMessage> messages) {
        contents = messages;
    }

    @Override
    public ArrayList<TextVector> getVecs() {
        Set<Message> messages = new HashSet<>();
        for (ClusterableObject o : contents)
            messages.add(((ClusterableMessage)o).getMessage());

        try {
            return new ArrayList<>(GenericClusterer.getVectoriser().doc2vec(messages));
        } catch (BatchSizeTooSmallException e) {
            return null;
        }
    }
}
