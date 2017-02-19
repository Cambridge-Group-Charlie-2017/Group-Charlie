package uk.ac.cam.cl.charlie.clustering;

import java.util.ArrayList;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableWordAndOccurence;
import uk.ac.cam.cl.charlie.math.Vector;

/**
 * Created by Ben on 13/02/2017.
 */
public class ClusterableWordGroup extends ClusterableObjectGroup {
    private ArrayList<ClusterableWordAndOccurence> contents;

    public ClusterableWordGroup(ArrayList<ClusterableWordAndOccurence> conts) {
	this.contents = conts;
    }

    @Override
    public ArrayList<Vector> getVecs() {

	return null;
    }
}
