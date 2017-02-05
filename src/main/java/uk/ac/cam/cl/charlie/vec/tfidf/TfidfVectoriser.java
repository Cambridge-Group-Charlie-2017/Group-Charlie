package uk.ac.cam.cl.charlie.vec.tfidf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.vec.Document;
import uk.ac.cam.cl.charlie.vec.Email;
import uk.ac.cam.cl.charlie.vec.Vector;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;

/**
 * Created by Shyam Tailor on 04/02/2017.
 */
public class TfidfVectoriser implements VectorisingStrategy {
    private Word2Vec model;
    private boolean modelLoaded;
    private String word2vecPath = "src/main/res/word2vec/wordvectors.bin";
    private Tfidf tf = null;
    private Database database = null; //Is this the database we store our results in, according to our meeting 3rd Feb?
    
    private int vectorDimensions = 300;

    public Optional<ArrayList<Double>> word2vec(String word) {
        // using optional here since a word not being in the vocab is hardly an "exceptional" case
    	// ^ we cannot use Optional with double[] as Optional uses generics which require a class.
        if (model.hasWord(word)) {
            return Optional.of(new ArrayList<Double>());
        }
        else {
        	//Do we really not want to attempt to vectorise the word at all?
            return Optional.empty();
        }
    }
    
    public void giveDatabaseInstance(Database database) {
    	this.database = database;
    }
    
    public boolean hasDatabaseInstance() {
    	return database != null ? true : false;
    }

    @Override
    public Vector doc2vec(Document doc) {
        return null;
    }

    @Override
    public Vector doc2vec(Email doc) {
        return null;
    }

    public boolean isModelLoaded() {
        return modelLoaded;
    }

    public void persistModel() {
        if (!modelLoaded) {
            return;
        }

        else {
            WordVectorSerializer.writeWord2VecModel(model, word2vecPath);
            modelLoaded = false;
        }
    }

    public void loadModel() {
        if (modelLoaded) {
            return;
        }

        else {
            model = WordVectorSerializer.readWord2VecModel(word2vecPath);
            modelLoaded = true;
        }
    }

    private double[] calculateDocVector(String text) {
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
            Optional<ArrayList<Double>> wordVec = word2vec(w);
            if (wordVec.isPresent()) {
                double weighting = calculateTFValue(w, text);
                totalWeight += weighting;

                for (int i = 0; i < vectorDimensions; ++i) {
                    docVector[i] += weighting * wordVec.get().get(i);
                }
            }
        }

        // divide by the total weight:
        for (int i = 0; i < vectorDimensions; ++i) {
            docVector[i] = docVector[i] / totalWeight;
        }

        return docVector;
    }

    private double calculateTFValue(String word, String doc) {
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
