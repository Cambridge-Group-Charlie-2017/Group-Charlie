package uk.ac.cam.cl.charlie.vec;

/*
 * Any algorithm used to transform words or emails or documents to feature vectors
 * should extend this class.
 * 
 * This will allow to easily test alternative algorithms and change them.
 */
public interface VectorisingStrategy {

	public Vector word2vec(Word word);
	
	public Vector doc2vec(Document doc);
	
	public Vector doc2vec(Email doc);
	
}
