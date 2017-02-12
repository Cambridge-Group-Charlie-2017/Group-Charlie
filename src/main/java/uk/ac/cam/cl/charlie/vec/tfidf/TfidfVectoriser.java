package uk.ac.cam.cl.charlie.vec.tfidf;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.deeplearning4j.models.embeddings.loader.VectorsConfiguration;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.embeddings.wordvectors.WordVectors;
import org.deeplearning4j.models.word2vec.Word2Vec;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.vec.Document;
import uk.ac.cam.cl.charlie.vec.Email;
import uk.ac.cam.cl.charlie.vec.TextVector;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;

/**
 * Created by Shyam Tailor on 04/02/2017.
 */
public class TfidfVectoriser implements VectorisingStrategy {
    private Word2Vec model;
    private boolean modelLoaded;
    private String word2vecPath = "src/main/resources/word2vec/wordvectors.txt";
    private Tfidf tf = null;

    private final int vectorDimensions = 300;

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

    @Override
    public TextVector doc2vec(Email doc) {
        // todo add anything that is relevant to the email header here.
        return doc2vec(doc.getTextBody());
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public void persistModel() throws TfidfException {
        if (!modelLoaded) {
            return;
        }

        else {
            try {
                WordVectorSerializer.writeWordVectors(model.getLookupTable(), new File(word2vecPath));
            } catch (IOException e) {
                e.printStackTrace();
                modelLoaded = false;
                throw new TfidfException();
            }
            modelLoaded = false;
        }
    }

    // load google model is deprecated in favour of a more general method (which doesn't work!)
    @SuppressWarnings("deprecation")
    public void loadModel() throws TfidfException {
        if (modelLoaded) {
            return;
        }

        else {
            try {
                model = WordVectorSerializer.loadGoogleModel(new File(word2vecPath), false, true);
            } catch (IOException e) {
                e.printStackTrace();
                modelLoaded = false;
                throw new TfidfException();
            }
            modelLoaded = true;
        }
    }

    private double[] calculateDocVector(String text) throws SQLException {
        // weighted average of word vectors using tdidf

        // don't want to duplicate words -> use a set
        Set<String> words = new HashSet<String>();

        for (String w : text.split("[\\W]")) { //this will split on non-word characters
            words.add(w);
        }

        double[] docVector = new double[vectorDimensions];
        double totalWeight = 0.0;

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
        for (String w : doc.split("[\\W]")) { //this will split on non-word characters
            if (w.equals(word)) {
                ++count;
            }
        }

        return count * Math.log((double)tf.totalNumberDocuments() / tf.numberOfDocsWithWith(word));
    }
}
