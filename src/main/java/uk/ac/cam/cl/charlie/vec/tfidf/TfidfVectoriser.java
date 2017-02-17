package uk.ac.cam.cl.charlie.vec.tfidf;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.charlie.mail.Messages;
import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.BatchSizeTooSmallException;
import uk.ac.cam.cl.charlie.vec.Document;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;
import uk.ac.cam.cl.charlie.vec.tfidf.kvstore.WordVecDB;

/**
 * Created by Shyam Tailor on 04/02/2017.
 */
public class TfidfVectoriser implements VectorisingStrategy {

    private static Logger log = LoggerFactory.getLogger(TfidfVectoriser.class);

    private String word2vecPath = "src/main/resources/word2vec/wordvectors.txt";

    private CachedWordCounter globalCounter;
    private WordVecDB vectorDB;

    // An empty string is filter when counting words, so it is safe to use here.
    private static String TOTAL_NUMBER_OF_DOCS = "";
    private HashMap<Message, Vector> vectorMap = new HashMap<>();

    private final int vectorDimensions = 300;

    // Using singleton pattern.
    private static TfidfVectoriser singleton;

    public static TfidfVectoriser getVectoriser() {
        if (singleton == null)
            singleton = new TfidfVectoriser();
        return singleton;
    }

    private TfidfVectoriser() {
        globalCounter = new CachedWordCounter(PersistentWordCounter.getInstance());
        vectorDB = WordVecDB.getInstance();
    }

    @Override
    public Optional<Vector> word2vec(String word) {
        // using optional here since a word not being in the vocab is hardly an
        // "exceptional" case

        Vector v = vectorDB.get(word);
        if (v == null) {
            return Optional.of(v);
        } else {
            return Optional.empty();
        }
    }

    private void train(Document doc) {
        BasicWordCounter docCounter = BasicWordCounter.count(doc.getContent());

        for (String w : docCounter.words()) {
            globalCounter.increment(w);
        }

        globalCounter.increment(TOTAL_NUMBER_OF_DOCS);
    }

    @Override
    public Vector doc2vec(Document doc) {
        train(doc);
        // todo add any other content to do with names or other meta data
        return calculateDocVector(doc.getContent());
    }

    // Provide a method for batching vectorisation, which calls load() and
    // close().
    @Override
    public List<Vector> doc2vec(List<Message> emailBatch) throws BatchSizeTooSmallException {
        if (emailBatch == null) {
            return null;
        }
        log.info("Starting vectorizing batch of size {}", emailBatch.size());
        try {
            List<Vector> vectorBatch = new ArrayList<>();
            List<Document> intermediateBatch = new ArrayList<>();
            // not sure if msg.getFileName() is appropriate here. Feel free to
            // change to msg.getSubject() or something.
            // Also, for the actual Message objects we're going to use (if we
            // don't use MimeMessage),
            // there may be different method calls for getting the body content
            // as a String.
            for (Message msg : emailBatch) {
                String body = Messages.getBodyText(msg);
                Document doc = new Document(msg.getSubject(), body);
                train(doc);
                intermediateBatch.add(doc);
            }
            log.info("Model trained");
            // Checks if sufficient emails are in the database
            if (globalCounter.frequency(TOTAL_NUMBER_OF_DOCS) < 20) {
                throw new BatchSizeTooSmallException();
            }
            for (Document doc : intermediateBatch) {
                vectorBatch.add(calculateDocVector(doc.getContent()));
            }
            log.info("Batch vectorized");
            return vectorBatch;
        } catch (MessagingException | IOException e) {
            return null;
        } catch (BatchSizeTooSmallException e) {
            System.err.println("Batch size was too small. Tfidf needs at least 20 Messages.");
            return null;
        }
    }

    @Override
    public Vector doc2vec(Message msg) throws BatchSizeTooSmallException {
        // todo add anything that is relevant to the email header here.
        try {
            // load();
            // not sure if msg.getFileName() is appropriate here. Feel free to
            // change to msg.getSubject() or something.
            // Also, for the actual Message objects we're going to use (if we
            // don't use MimeMessage),
            // there may be different method calls for getting the body content
            // as a String.
            String body = Messages.getBodyText(msg);
            // Checks if sufficient emails are in the database
            if (globalCounter.frequency(TOTAL_NUMBER_OF_DOCS) < 20) {
                throw new BatchSizeTooSmallException();
            }

            Vector result = doc2vec(new Document(msg.getSubject(), body));
            vectorMap.put(msg, result);
            return result;
        } catch (MessagingException | IOException e) {
            return null;
        } catch (BatchSizeTooSmallException e) {
            System.err.println("Batch size was too small. Tfidf needs at least 20 Messages.");
            return null;
        } finally {
            // close();
        }
    }

    @Override
    public void load() {
        return; // not needed with the new backend - kept for compatability purposes
    }

    @Override
    public void close() {
        return; // again, as with load, not needed
    }

    @Override
    public boolean ready() {
        return true; // in theory should always work (unless the db is broken).
    }

    private Vector calculateDocVector(String text) {
        // weighted average of word vectors using tdidf

        BasicWordCounter words = BasicWordCounter.count(text);

        Vector vec = Vector.zero(vectorDimensions);

        double totalDocs = globalCounter.frequency(TOTAL_NUMBER_OF_DOCS);

        for (String w : words.words()) {
            Optional<Vector> wordVec = word2vec(w);
            if (wordVec.isPresent()) {
                double totalDocsWith = globalCounter.frequency(w);
                double weighting = words.frequency(w) * Math.log(totalDocs / totalDocsWith);

                vec = vec.add(wordVec.get().scale(weighting));
            }
        }

        return vec.normalize();
    }

}
