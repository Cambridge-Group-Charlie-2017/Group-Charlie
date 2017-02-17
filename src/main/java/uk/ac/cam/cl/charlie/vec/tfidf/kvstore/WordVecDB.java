package uk.ac.cam.cl.charlie.vec.tfidf.kvstore;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.factory.Nd4j;
import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.db.Serializers;
import uk.ac.cam.cl.charlie.math.Vector;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Created by shyam on 15/02/2017.
 */
public final class WordVecDB {
    // todo: possibility of batch insertion into the db (documentation is a tad awful on this)
    private static WordVecDB instance;

    private Cache<String, Vector> cache;

    private Database db;
    PersistentMap<String, Vector> map;
    String vectorDBName = "vectors";

    private class VectorSerialiser extends Serializer<Vector> {
        @Override
        public boolean typecheck(Object obj) {
            return obj instanceof Vector;
        }

        @Override
        public byte[] serialize(Vector object) {
            // there might be a quicker way to do this...
            ByteBuffer buf = ByteBuffer.allocate(object.size() * 8);
            double[] components = object.toDoubleArray();

            for (int i = 0; i < object.size(); ++i) {
                buf.putDouble(components[i]);
            }
            return buf.array();
        }

        @Override
        public Vector deserialize(byte[] bytes) {
            return new Vector(ByteBuffer.wrap(bytes).asDoubleBuffer().array());
        }
    }

    private WordVecDB() {
        cache = CacheBuilder.newBuilder()
                .maximumSize(2000)
                .build();
        db = Database.getInstance();
        map = db.getMap(vectorDBName, Serializers.STRING, new VectorSerialiser());
    }

    public static WordVecDB getInstance() {
        if (instance == null) {
            instance = new WordVecDB();
        }
        return instance;
    }

    private static void populateFromTextFile() {
        // todo testing
        WordVecDB db = WordVecDB.getInstance();
        File vectorFile = new File("src/main/resources/word2vec/wordvectors.txt");
        try (BufferedReader br = new BufferedReader(new FileReader(vectorFile))) {
            String line = br.readLine();
            String[] header = line.split(" ");
            int nOfWords = Integer.getInteger(header[0]);
            int nDimensions = Integer.getInteger(header[1]);

            line = br.readLine();
            for (int i = 0; i < nOfWords; ++i) {
                if (line == null) {
                    // malformed text file
                    throw new Error("Text file loading vectors is malformed - less words than header specifies");
                }
                String[] tokens = line.split(" ");
                String word = tokens[0];
                double[] components = new double[nDimensions];
                for (int j = 0; j < nDimensions; ++j) {
                    components[j] = Double.valueOf(tokens[j + 1]);
                }
                db.put(word, new Vector(components));

            }
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new Error("Malformed text file - dimensions incorrect", e);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public void wipeDB() {
        map.clear();
    }

    public void put(String w, Vector v) {
        cache.put(w, v);
        map.put(w, v);
    }

    public Vector get(String w) {
        try {
            return cache.get(w, new Callable<Vector>() {
                @Override
                public Vector call() throws Exception {
                    return map.get(w);
                }
            });
        } catch (ExecutionException e) {
            throw new Error(e);
        }
    }

    public void delete(String w) {
        cache.invalidate(w);
        map.remove(w);
    }

    public Word2Vec getModelForTraining() {
        WeightLookupTable table = new InMemoryLookupTable();

        for (Map.Entry<String, Vector> entry : map.entrySet()) {
            table.putVector(entry.getKey(), Nd4j.create(entry.getValue().toDoubleArray()));
        }

        // if this doesn't work there is an alternative way of doing it with the Builder class
        Word2Vec w = new Word2Vec();
        w.setLookupTable(table);
        return w;
    }

    public void createDBFromModel(Word2Vec w2v) {
        // todo
    }
}
