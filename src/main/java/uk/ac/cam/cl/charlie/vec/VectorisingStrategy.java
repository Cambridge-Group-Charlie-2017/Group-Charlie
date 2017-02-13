package uk.ac.cam.cl.charlie.vec;

import javax.mail.Message;
import java.util.Optional;
import java.util.Set;

/**
 * Any algorithm used to transform words or emails or documents to feature vectors
 * should extend this class.
 * 
 * This will allow to easily test alternative algorithms and change them.
 */
public interface VectorisingStrategy {

	public Optional<TextVector> word2vec(String word);
	
	public TextVector doc2vec(Document doc) throws BatchSizeTooSmallException;
	
	public Set<TextVector> doc2vec(Set<Message> batch) throws BatchSizeTooSmallException;

	public TextVector doc2vec(Message msg) throws BatchSizeTooSmallException;

	// load and close need to be called before the above functions work
	public void load();
	public void close();
	public boolean ready();
	
}
