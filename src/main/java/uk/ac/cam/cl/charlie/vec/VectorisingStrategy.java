package uk.ac.cam.cl.charlie.vec;

import java.sql.SQLException;
import java.util.Optional;

/**
 * Any algorithm used to transform words or emails or documents to feature vectors
 * should extend this class.
 * 
 * This will allow to easily test alternative algorithms and change them.
 */
public interface VectorisingStrategy {

	public Optional<TextVector> word2vec(String word);
	
	public TextVector doc2vec(Document doc) throws SQLException;
	
	public TextVector doc2vec(Email doc) throws SQLException;
	
}
