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
    private final int minBatchSize = 20;

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
        return vectorDB.get(word);

    }

    // This method should only be called on subject header
    // It is does not increment the number of documents as it is part of the
    // email message.
    private void train(String emailsubject) {
        BasicWordCounter counter = BasicWordCounter.count(emailsubject);

        for (String w : counter.words()) {
            globalCounter.increment(w);
        }
    }

    private void train(Document doc) {
        BasicWordCounter docCounter = BasicWordCounter.count(doc.getContent());

        for (String w : docCounter.words()) {
            globalCounter.increment(w);
        }

        globalCounter.increment(TOTAL_NUMBER_OF_DOCS);
    }

    private void train(Message msg) throws MessagingException, IOException {
        BasicWordCounter counter = BasicWordCounter.count(Messages.getBodyText(msg));

        for (String w : counter.words()) {
            globalCounter.increment(w);
        }

        globalCounter.increment(TOTAL_NUMBER_OF_DOCS);
    }

    //To be used for metadata such as subject of a document or email.
    public Vector sent2vec(String subject) {
        train(subject);

        return calculateDocVector(subject);
    }

    @Override
    public Vector doc2vec(Document doc) {
        train(doc);
        // todo add any other content to do with names or other meta data
        return calculateDocVector(doc.getContent());
    }

    @Override
    public List<Vector> documentBatch2vec(List<Document> batch) throws BatchSizeTooSmallException {
        if (batch == null) {
            return null;
        }

        log.info("Starting vectorising batch of size {}", batch.size());

        List<Vector> vectorBatch = new ArrayList<>();
        for (Document d : batch) {
            train(d);
        }
        log.info("Model trained with word counts");

        if (globalCounter.frequency(TOTAL_NUMBER_OF_DOCS) < minBatchSize) {
            throw new BatchSizeTooSmallException();
        }

        int count = 0;
        for (Document d : batch) {
            ++count;
            vectorBatch.add(calculateDocVector(d.getContent()));
        }
        log.info("Batch vectorised");
        return vectorBatch;
    }

    // Provide a method for batching vectorisation
    @Override
    public List<Vector> emailBatch2vec(List<Message> emailBatch) throws BatchSizeTooSmallException {
        if (emailBatch == null) {
            return null;
        }
        log.info("Starting vectorizing batch of size {}", emailBatch.size());
        try {
            // this.load();
            List<Vector> vectorBatch = new ArrayList<>();
            // not sure if msg.getFileName() is appropriate here. Feel free to
            // change to msg.getSubject() or something.
            // Also, for the actual Message objects we're going to use (if we
            // don't use MimeMessage),
            // there may be different method calls for getting the body content
            // as a String.
            for (Message msg : emailBatch) {
                String body = Messages.getBodyText(msg);
                train(msg);
            }
            log.info("Model trained");
            // Checks if sufficient emails are in the database
            if (globalCounter.frequency(TOTAL_NUMBER_OF_DOCS) < minBatchSize) {
                throw new BatchSizeTooSmallException();
            }
            int count = 0;
            for (Message msg : emailBatch) {
                ++count;
                // Adding the Subject to the feature vector passed to the clusterer
                // Done by concatening the Subject vector with the textbody vector
                // An alternative solution would be to take the weighted average
                // To decide which method is best, testing is required
                Vector head = sent2vec(msg.getSubject());
                Vector tail = calculateDocVector(Messages.getBodyText(msg));
                vectorBatch.add(Vector.concat(head, tail));
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
    public Vector emailBatch2vec(Message msg) throws BatchSizeTooSmallException {
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
            // Adding the Subject to the feature vector passed to the clusterer
            // Done by concatening the Subject vector with the textbody vector
            // An alternative solution would be to take the weighted average
            // To decide which method is best, testing is required
            Vector tail = calculateDocVector(body);
            Vector head = sent2vec(msg.getSubject());
            Vector result = Vector.concat(head, tail);
            vectorMap.put(msg, result);
            return result;
        } catch (MessagingException | IOException e) {
            return null;
        } catch (BatchSizeTooSmallException e) {
            System.err.println("Batch size was too small. Tfidf needs at least 20 Messages.");
            return null;
        }
    }

    private Vector calculateDocVector(String text) {
        // weighted average of word vectors using tfidf

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

    @Override
    public int getMinimumBatchSize() {
        return minBatchSize;
    }

    @Override
    public boolean minimumBatchSizeReached() {
        return globalCounter.frequency(TOTAL_NUMBER_OF_DOCS) >= minBatchSize;
    }
}