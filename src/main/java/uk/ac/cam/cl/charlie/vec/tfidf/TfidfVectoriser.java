package uk.ac.cam.cl.charlie.vec.tfidf;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import uk.ac.cam.cl.charlie.vec.*;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMultipart;

/**
 * Created by Shyam Tailor on 04/02/2017.
 */
public class TfidfVectoriser implements VectorisingStrategy {
    private Word2Vec model;
    private boolean modelLoaded;
    private String word2vecPath = "src/main/resources/word2vec/wordvectors.txt";
    private String regexSplit = "[^a-zA-Z0-9']+";
    private Tfidf tf;

    private final int vectorDimensions = 300;

    public TfidfVectoriser() {
        try {
            tf = Tfidf.getInstance();
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    public Optional<TextVector> word2vec(String word) {
        // using optional here since a word not being in the vocab is hardly an "exceptional" case
    	// ^ we cannot use Optional with double[] as Optional uses generics which require a class.
        if (model.hasWord(word)) {
            return Optional.of(new TextVector(model.getWordVector(word)));
        }
        else {
        	//Do we really not want to attempt to vectorise the word at all?
            // there's no data so you can't.

            // perhaps need to occasionally update the vocabulary and train the word2vec
            // model further on the user's stuff
            return Optional.empty();
        }
    }

    @Override
    public TextVector doc2vec(Document doc) {
        // todo add any other content to do with names or other meta data
        try {
            tf.addDocument(doc);
            return new TextVector(calculateDocVector(doc.getContent()));
        } catch (TfidfException e) {
            throw new Error(e);
        } catch (SQLException e) {
            throw new Error(e);
        }
    }

    
    //Provide a method for batching vectorisation, which calls load() and close().
    @Override
    public Set<TextVector> doc2vec(Set<Message> emailBatch) throws BatchSizeTooSmallException {
    	if(emailBatch == null) { return null; }
    	try {
    		
    		this.load();
    		Set<TextVector> vectorBatch = new HashSet<TextVector>();
    		Set<Document> intermediateBatch = new HashSet<Document>();
            //not sure if msg.getFileName() is appropriate here. Feel free to change to msg.getSubject() or something.
            //Also, for the actual Message objects we're going to use (if we don't use MimeMessage),
            //there may be different method calls for getting the body content as a String.
    		for(Message msg: emailBatch) {
            	MimeMultipart content = (MimeMultipart)msg.getContent();
            	String body = (String)content.getBodyPart(0).getContent();
            	Document doc = new Document(msg.getSubject(), body);
            	tf.addDocument(doc);
            	intermediateBatch.add(doc);
    		}
    		//Checks if sufficient emails are in the database
    		if(tf.totalNumberDocuments() < 20) { throw new BatchSizeTooSmallException(); }
    		for(Document doc: intermediateBatch) {
    			vectorBatch.add(new TextVector(calculateDocVector(doc.getContent())));
    		}
    		this.close();
            return vectorBatch;
        } catch (MessagingException | IOException | TfidfException | SQLException e) {
            return null;
        }
    }
    
    public TextVector doc2vec(Message msg) {
        // todo add anything that is relevant to the email header here.
        try {
            //not sure if msg.getFileName() is appropriate here. Feel free to change to msg.getSubject() or something.
            //Also, for the actual Message objects we're going to use (if we don't use MimeMessage),
            //there may be different method calls for getting the body content as a String.
            MimeMultipart content = (MimeMultipart)msg.getContent();
            String body = (String)content.getBodyPart(0).getContent();
            //Checks if sufficient emails are in the database
    		if(tf.totalNumberDocuments() < 20) { throw new BatchSizeTooSmallException(); }
            return doc2vec(new Document(msg.getSubject(), body));
        } catch (MessagingException | IOException |SQLException e) {
            return null;
		} catch (BatchSizeTooSmallException e) {
			System.err.println("Batch size was too small. Tfidf needs at least 20 Messages.");
			return null;
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
                File original = new File (word2vecPath);
                original.delete();
                writeTo.renameTo(original);

                tf.close();
            } catch (IOException | SQLException e) {
                modelLoaded = false;
                throw new Error(e);
            }
            modelLoaded = false;
        }
    }

    // load google model is deprecated in favour of a more general method (which doesn't work!)
    @Override
    @SuppressWarnings("deprecation")
    public void load() {
        if (modelLoaded && !tf.isClosed()) {
            return;
        }
        else {
            try {
                if (!modelLoaded) {
                    model = WordVectorSerializer.loadGoogleModel(new File(word2vecPath), false, true);
                }
                if (tf.isClosed()) {
                    tf = Tfidf.getInstance();
                }
            } catch (IOException | SQLException e) {
                modelLoaded = false;
                throw new Error(e);
            }
            modelLoaded = true;
        }
    }

    public boolean ready() {
        return modelLoaded && !tf.isClosed();
    }

    private double[] calculateDocVector(String text) throws SQLException {
        // weighted average of word vectors using tdidf

        // don't want to duplicate words -> use a set
        Set<String> words = new HashSet<String>();

        for (String w : text.split(regexSplit)) { //this will split on non-word characters
            words.add(w);
        }
        words.remove(""); //empty string should not be included.

        double[] docVector = new double[vectorDimensions];
        double totalWeight = 0.0;

        //TODO: Should take case into account. Convert to lower case?
        // add the vectors for every word which is in the vocab
        for (String w : words) {
            Optional<TextVector> wordVec = word2vec(w);
            if (wordVec.isPresent()) {
                double weighting = calculateTFValue(w, text);
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

    private double calculateTFValue(String word, String doc) throws SQLException {
        // see: https://deeplearning4j.org/bagofwords-tf-idf
        if (tf == null) {
            tf = Tfidf.getInstance();
        }

        // calculate the number of occurences of word in doc
        int count = 0;
        for (String w : doc.split(regexSplit)) { //this will split on non-word characters (hopefully)
            if (w.equals(word)) {
                ++count;
            }
        }

        return count * Math.log((double)tf.totalNumberDocuments() / tf.numberOfDocsWithWith(word));
    }
}
