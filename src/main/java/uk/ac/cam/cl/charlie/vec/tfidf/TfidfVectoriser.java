package uk.ac.cam.cl.charlie.vec.tfidf;

import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import uk.ac.cam.cl.charlie.vec.Document;
import uk.ac.cam.cl.charlie.vec.Email;
import uk.ac.cam.cl.charlie.vec.Vector;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Created by Shyam Tailor on 04/02/2017.
 */
public class TfidfVectoriser implements VectorisingStrategy {
    private Word2Vec model;
    private boolean modelLoaded;
    private String word2vecPath = "src/main/res/word2vec/wordvectors.bin";
    private Tfidf tf = null;

    private int vectorDimensions = 300;

    public Optional<double[]> word2vec(String word) {
        // using optional here since a word not being in the vocab is hardly an "exceptional" case
        if (model.hasWord(word)) {
            return Optional.of(double[]);
        }
        else {
            return Optional.empty();
        }
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

        for (String w : text.split(" ")) {
            words.add(w);
        }

        double[] docVector = new double[vectorDimensions];
        double totalWeight = 0.0;

        // add the vectors for every word which is in the vocab
        for (String w : words) {
            Optional<double[]> wordVec = word2vec(w);
            if (wordVec.isPresent()) {
                double weighting = calculateTFValue(w, text);
                totalWeight += weighting;

                for (int i = 0; i < vectorDimensions; ++i) {
                    docVector[i] += weighting * wordVec.get()[i];
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
        for (String w : doc.split(" ")) {
            if (w.equals(word)) {
                ++count;
            }
        }

        return count * Math.log((double)tf.totalNumberDocuments() / tf.numberOfDocsWithWith(word));
    }
}
