package uk.ac.cam.cl.charlie.vec;

import java.util.ArrayList;
import java.util.Optional;

/*
 * Any algorithm used to transform words or emails or documents to feature vectors
 * should extend this class.
 * 
 * This will allow to easily test alternative algorithms and change them.
 */
public interface VectorisingStrategy {

	public Optional<ArrayList<Double>> word2vec(String word);
	
	public Vector doc2vec(Document doc);
	
	public Vector doc2vec(Email doc);
	
}
