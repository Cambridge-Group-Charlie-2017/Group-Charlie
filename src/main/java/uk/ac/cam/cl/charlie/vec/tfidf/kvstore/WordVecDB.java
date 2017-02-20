package uk.ac.cam.cl.charlie.vec.tfidf.kvstore;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.deeplearning4j.models.word2vec.Word2Vec;
import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.db.Serializers;
import uk.ac.cam.cl.charlie.math.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Created by shyam on 15/02/2017.
 */
public final class WordVecDB {
    // todo: possibility of batch insertion into the db (documentation is a tad awful on this)
    private static WordVecDB instance;

    private Cache<String, Optional<Vector>> cache;

    private Database db;
    PersistentMap<String, Vector> map;
    String vectorDBName = "vectors";

    private static double[] toDoubleArray(byte[] bytes) {
        // needed for deserialise (inner classes can't have static methods)
        if (bytes.length % 8 != 0) {
            throw new IllegalArgumentException();
        }

        ByteBuffer buf = ByteBuffer.wrap(bytes);

        double[] a = new double[bytes.length / 8];

        for (int i = 0; i < a.length; ++i) {
            a[i] = buf.getDouble();
        }
        return a;
    }

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
            return new Vector(toDoubleArray(bytes));
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

    public static void populateFromTextFile(String fname) {
        // todo testing
        WordVecDB db = WordVecDB.getInstance();
        File vectorFile = new File(fname);

        try (BufferedReader br = new BufferedReader(new FileReader(vectorFile))) {
            String line = br.readLine();
            String[] header = line.split(" ");
            int nOfWords = Integer.parseInt(header[0]);
            int nDimensions = Integer.parseInt(header[1]);

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
                line = br.readLine();

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
        cache.put(w, Optional.of(v));
        map.put(w, v);
    }

    // google cache does not like null values
    public Optional<Vector> get(String w) {
        try {
            return cache.get(w, new Callable<Optional<Vector>>() {

                @Override
                public Optional<Vector> call() throws Exception {
                    Vector v = map.get(w);
                    if (v == null) {
                        return Optional.empty();
                    }

                    return Optional.of(v);
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
        // This method requires further implementation on the database side
        // hence forget about it for now
        throw new UnsupportedOperationException();


//        WeightLookupTable table = new InMemoryLookupTable();
//
//        for (Map.Entry<String, Vector> entry : map.entrySet()) {
//            table.putVector(entry.getKey(), Nd4j.create(entry.getValue().toDoubleArray()));
//        }
//
//        // if this doesn't work there is an alternative way of doing it with the Builder class
//        Word2Vec w = new Word2Vec();
//        w.setLookupTable(table);
//        return w;
    }

    public void createDBFromModel(Word2Vec w2v) {
        // todo
    }
}
