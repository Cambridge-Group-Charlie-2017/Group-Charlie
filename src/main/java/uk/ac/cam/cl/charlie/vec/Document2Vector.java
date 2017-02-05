package uk.ac.cam.cl.charlie.vec;

import java.util.ArrayList;
import java.util.Optional;

/*
 * Document2Vector is the interface to be used by Clusterer and Classifier
 * to convert email or/and their attachements to feature vectors in a vector
 * space model (VSM).
 * 
 * The implementation of the algorithm(s) used for this class should follow the
 * Strategy design pattern. This class will be the context.
 * @author Shyam Tailor and Louis-Pascal Xhonneux
 */
public class Document2Vector {
	
	private VectorisingStrategy strategy;
	
	public Document2Vector(final VectorisingStrategy strategy) {
		this.strategy = strategy;
	}
	
	public Optional<ArrayList<Double>> word2vec(String word) {
		return strategy.word2vec(word);
	}
	
}
