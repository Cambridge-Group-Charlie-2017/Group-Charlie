package uk.ac.cam.cl.charlie.vec.tfidf;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import uk.ac.cam.cl.charlie.mail.Messages;
import uk.ac.cam.cl.charlie.ui.MailUtil;
import uk.ac.cam.cl.charlie.vec.BatchSizeTooSmallException;
import uk.ac.cam.cl.charlie.vec.Document;
import uk.ac.cam.cl.charlie.vec.TextVector;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;

/**
 * Created by Shyam Tailor on 04/02/2017.
 */
public class TfidfVectoriser implements VectorisingStrategy {
    private Word2Vec model;
    private boolean modelLoaded;
    private String word2vecPath = "src/main/resources/word2vec/wordvectors.txt";

    private CachedWordCounter globalCounter;

    private static String TOTAL_NUMBER_OF_DOCS = "totalnumberofdocstfidf";
    private HashMap<Message, TextVector> vectorMap = new HashMap<>();

    private final int vectorDimensions = 300;

    // Using singleton pattern.
    private static TfidfVectoriser singleton;

    public static TfidfVectoriser getVectoriser() {
	if (singleton == null)
	    singleton = new TfidfVectoriser();
	return singleton;
    }

    private TfidfVectoriser() {
	// added load() to constructor for performance reasons for testing.
	load();

	globalCounter = new CachedWordCounter(PersistentWordCounter.getInstance());
    }

    @Override
    public Optional<TextVector> word2vec(String word) {
	// using optional here since a word not being in the vocab is hardly an
	// "exceptional" case
	// ^ we cannot use Optional with double[] as Optional uses generics
	// which require a class.
	if (model.hasWord(word)) {
	    return Optional.of(new TextVector(model.getWordVector(word)));
	} else {
	    // Do we really not want to attempt to vectorise the word at all?
	    // there's no data so you can't.

	    // perhaps need to occasionally update the vocabulary and train the
	    // word2vec
	    // model further on the user's stuff
	    return Optional.empty();
	}
    }

    private void train(Document doc) {
	BasicWordCounter docCounter = BasicWordCounter.count(doc.getContent());

	for (String w : docCounter.words()) {
	    globalCounter.increment(w, docCounter.frequency(w));
	}

	globalCounter.increment(TOTAL_NUMBER_OF_DOCS);
    }

    @Override
    public TextVector doc2vec(Document doc) {
	train(doc);
	globalCounter.synchronize();
	// todo add any other content to do with names or other meta data
	try {
	    return new TextVector(calculateDocVector(doc.getContent())).normalize();
	} catch (SQLException e) {
	    throw new Error(e);
	}
    }

    // Provide a method for batching vectorisation, which calls load() and
    // close().
    @Override
    public List<TextVector> doc2vec(List<Message> emailBatch) throws BatchSizeTooSmallException {
	if (emailBatch == null) {
	    return null;
	}
	try {
	    // this.load();
	    List<TextVector> vectorBatch = new ArrayList<>();
	    List<Document> intermediateBatch = new ArrayList<>();
	    // not sure if msg.getFileName() is appropriate here. Feel free to
	    // change to msg.getSubject() or something.
	    // Also, for the actual Message objects we're going to use (if we
	    // don't use MimeMessage),
	    // there may be different method calls for getting the body content
	    // as a String.
	    for (Message msg : emailBatch) {
		Part body = Messages.getBodyPart(msg, false);
		Document doc = new Document(msg.getSubject(), (String) body.getContent());
		train(doc);
		intermediateBatch.add(doc);
	    }
	    globalCounter.synchronize();
	    // Checks if sufficient emails are in the database
	    if (globalCounter.frequency(TOTAL_NUMBER_OF_DOCS) < 20) {
		throw new BatchSizeTooSmallException();
	    }
	    for (Document doc : intermediateBatch) {
		vectorBatch.add(new TextVector(calculateDocVector(doc.getContent())).normalize());
	    }

	    return vectorBatch;
	} catch (MessagingException | IOException | SQLException e) {
	    return null;
	} catch (BatchSizeTooSmallException e) {
	    System.err.println("Batch size was too small. Tfidf needs at least 20 Messages.");
	    return null;
	} finally {
	    // this.close();
	}
    }

    @Override
    public TextVector doc2vec(Message msg) throws BatchSizeTooSmallException {
	// todo add anything that is relevant to the email header here.
	try {
	    // load();
	    // not sure if msg.getFileName() is appropriate here. Feel free to
	    // change to msg.getSubject() or something.
	    // Also, for the actual Message objects we're going to use (if we
	    // don't use MimeMessage),
	    // there may be different method calls for getting the body content
	    // as a String.
	    String body = MailUtil.getBody(msg, false).content;
	    // Checks if sufficient emails are in the database
	    if (globalCounter.frequency(TOTAL_NUMBER_OF_DOCS) < 20) {
		throw new BatchSizeTooSmallException();
	    }

	    TextVector result = doc2vec(new Document(msg.getSubject(), body));
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
    public void close() {
	if (!modelLoaded) {
	    return;
	}

	else {
	    try {
		File writeTo = new File(word2vecPath + ".bak");
		WordVectorSerializer.writeWordVectors(model.getLookupTable(), writeTo);
		File original = new File(word2vecPath);
		original.delete();
		writeTo.renameTo(original);

	    } catch (IOException e) {
		modelLoaded = false;
		throw new Error(e);
	    }
	    modelLoaded = false;
	}
    }

    // load google model is deprecated in favour of a more general method (which
    // doesn't work!)
    @Override
    @SuppressWarnings("deprecation")
    public void load() {
	if (modelLoaded) {
	    return;
	} else {
	    try {
		if (!modelLoaded) {
		    model = WordVectorSerializer.loadGoogleModel(new File(word2vecPath), false, true);
		}
	    } catch (IOException e) {
		modelLoaded = false;
		throw new Error(e);
	    }
	    modelLoaded = true;
	}
    }

    @Override
    public boolean ready() {
	return modelLoaded;
    }

    private double[] calculateDocVector(String text) throws SQLException {
	// weighted average of word vectors using tdidf

	BasicWordCounter words = BasicWordCounter.count(text);

	double[] docVector = new double[vectorDimensions];
	double totalWeight = 0.0;

	// TODO: Should take case into account. Convert to lower case?
	// add the vectors for every word which is in the vocab
	for (String w : words.words()) {
	    Optional<TextVector> wordVec = word2vec(w);
	    if (wordVec.isPresent()) {
		double weighting = calculateTFValue(w, words.frequency(w), text);
		totalWeight += weighting;

		for (int i = 0; i < vectorDimensions; ++i) {
		    docVector[i] += weighting * wordVec.get().getRawComponents()[i];
		}
	    }
	}

	// divide by the total weight:
	for (int i = 0; i < vectorDimensions; ++i) {
	    docVector[i] = docVector[i] / totalWeight;
	}

	return docVector;
    }

    private double calculateTFValue(String word, int count, String doc) throws SQLException {
	// see: https://deeplearning4j.org/bagofwords-tf-idf
	double totalDocs = globalCounter.frequency(TOTAL_NUMBER_OF_DOCS);
	double totalDocsWith = globalCounter.frequency(word);
	return count * Math.log(totalDocs / totalDocsWith);
    }
}
