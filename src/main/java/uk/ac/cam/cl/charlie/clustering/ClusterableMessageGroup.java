package uk.ac.cam.cl.charlie.clustering;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;

import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.BatchSizeTooSmallException;

/**
 * Created by Ben on 13/02/2017.
 */
public class ClusterableMessageGroup extends ClusterableObjectGroup {

    // protected ArrayList<ClusterableMessage> contents;

    public ClusterableMessageGroup(ArrayList<ClusterableMessage> messages) {
	contents = new ArrayList<>();
	for (int i = 0; i < messages.size(); i++)
	    contents.add(messages.get(i));
    }

    @Override
    public List<Vector> getVecs() {
	ArrayList<Message> messages = new ArrayList<>();
	for (ClusterableObject o : contents)
	    messages.add(((ClusterableMessage) o).getMessage());

	try {
	    return GenericClusterer.getVectoriser().doc2vec(messages);
	} catch (BatchSizeTooSmallException e) {
	    return null;
	}
    }
}
