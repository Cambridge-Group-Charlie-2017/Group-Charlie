package uk.ac.cam.cl.charlie.vec;

import java.util.List;
import java.util.Optional;

import javax.mail.Message;

import uk.ac.cam.cl.charlie.math.Vector;

/**
 * Any algorithm used to transform words or emails or documents to feature
 * vectors should extend this class.
 *
 * This will allow to easily test alternative algorithms and change them.
 */
public interface VectorisingStrategy {

    public Optional<Vector> word2vec(String word);

    public Vector emailBatch2vec(Document doc) throws BatchSizeTooSmallException;

    public List<Vector> emailBatch2vec(List<Message> batch) throws BatchSizeTooSmallException;
    public List<Vector> documentBatch2vec(List<Document> batch) throws BatchSizeTooSmallException;

    public Vector emailBatch2vec(Message msg) throws BatchSizeTooSmallException;

    public boolean minimumBatchSizeReached();
    public int getMinimumBatchSize();

}
