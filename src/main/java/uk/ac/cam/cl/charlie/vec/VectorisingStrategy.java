package uk.ac.cam.cl.charlie.vec;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    void train(Message message);

    default void train(List<Message> batch) {
        batch.forEach(this::train);
    }

    public Vector doc2vec(Document doc);

    public Vector doc2vec(Message msg);

    default List<Vector> doc2vec(List<Message> batch) {
        return batch.stream().map(this::doc2vec).collect(Collectors.toList());
    }
    
    default List<Vector> batchdoc2vec(List<Document> batch) {
        return batch.stream().map(this::doc2vec).collect(Collectors.toList());
    }

	int getMinimumBatchSize();

	boolean minimumBatchSizeReached();

}
