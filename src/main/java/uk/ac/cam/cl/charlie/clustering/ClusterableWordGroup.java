package uk.ac.cam.cl.charlie.clustering;

import uk.ac.cam.cl.charlie.vec.TextVector;

import java.util.ArrayList;

/**
 * Created by Ben on 13/02/2017.
 */
public class ClusterableWordGroup extends ClusterableObjectGroup{
    private ArrayList<ClusterableWordAndOccurence> contents;

    public ClusterableWordGroup(ArrayList<ClusterableWordAndOccurence> conts) {
        this.contents = conts;
    }

    @Override
    public ArrayList<TextVector> getVecs() {

        return null;
    }
}
