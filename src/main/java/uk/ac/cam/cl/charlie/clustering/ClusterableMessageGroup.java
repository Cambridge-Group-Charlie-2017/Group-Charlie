package uk.ac.cam.cl.charlie.clustering;

import java.util.ArrayList;
import java.util.List;

import javax.mail.Message;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;

/*
 * Created by Ben on 13/02/2017.
 */
public class ClusterableMessageGroup extends ClusterableObjectGroup {

    public ClusterableMessageGroup(ArrayList<ClusterableMessage> messages) {
        contents = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++)
            contents.add(messages.get(i));
    }

    @Override
    public List<Vector> getVecs() {
        VectorisingStrategy strategy = Clusterer.getVectoriser();
        ArrayList<Message> messages = new ArrayList<>();
        for (ClusterableObject o : contents) {
            messages.add(((ClusterableMessage) o).getMessage());
        }

        strategy.train(messages);

        List<Vector> vector = new ArrayList<>();
        for (ClusterableObject o : contents) {
            vector.add(o.getVector());
        }

        return vector;
    }
}
