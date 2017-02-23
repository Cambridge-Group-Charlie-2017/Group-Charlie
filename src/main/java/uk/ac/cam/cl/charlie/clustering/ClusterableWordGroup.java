package uk.ac.cam.cl.charlie.clustering;

import java.util.*;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableWordAndOccurence;
import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;

/**
 * Created by Ben on 13/02/2017.
 */
public class ClusterableWordGroup extends ClusterableObjectGroup {
    //private ArrayList<ClusterableWordAndOccurence> contents;

    public ClusterableWordGroup(ArrayList<ClusterableWordAndOccurence> conts) {
	    contents = new ArrayList<ClusterableObject>();
        for(int i=0; i<conts.size();i++){
	        contents.add(conts.get(i));
        }
    }

    @Override
    public ArrayList<Vector> getVecs() {
        ArrayList<Vector> vecs = new ArrayList<Vector>();
        TfidfVectoriser vectoriser = TfidfVectoriser.getVectoriser();
        Iterator<ClusterableObject> iterator = contents.iterator();
        while(iterator.hasNext()) {
            ClusterableObject obj = iterator.next();
            Optional<Vector> vector = vectoriser.word2vec(((ClusterableWordAndOccurence)obj).getWord());
            if(vector.isPresent())
                vecs.add(vector.get());
            else
                iterator.remove();
        }
	    return vecs;
    }
}
