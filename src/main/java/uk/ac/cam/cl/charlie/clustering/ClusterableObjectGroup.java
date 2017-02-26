package uk.ac.cam.cl.charlie.clustering;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.math.Vector;

/**
 * Created by Ben on 13/02/2017.
 */

// This is needed because the vectorisation calls are different for different
// clusterable object types.
public abstract class ClusterableObjectGroup {
    protected ArrayList<ClusterableObject> contents;

    // Batch vectorise them
    public abstract List<Vector> getVecs();

    public ArrayList<ClusterableObject> getContents() {
	return contents;
    }

    public int size() {
	    return contents.size();
    }

    public ClusterableObject get(int i) {
	return contents.get(i);
    }
}
