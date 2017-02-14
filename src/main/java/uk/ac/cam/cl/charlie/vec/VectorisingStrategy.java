package uk.ac.cam.cl.charlie.vec;

import java.util.List;
import java.util.Optional;

import javax.mail.Message;

/**
 * Any algorithm used to transform words or emails or documents to feature
 * vectors should extend this class.
 *
 * This will allow to easily test alternative algorithms and change them.
 */
public interface VectorisingStrategy {

    public Optional<TextVector> word2vec(String word);

    public TextVector doc2vec(Document doc) throws BatchSizeTooSmallException;

    public List<TextVector> doc2vec(List<Message> batch) throws BatchSizeTooSmallException;

    public TextVector doc2vec(Message msg) throws BatchSizeTooSmallException;

    // load and close need to be called before the above functions work
    public void load();

    public void close();

    public boolean ready();

}
