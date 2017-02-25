package uk.ac.cam.cl.charlie.vec.tfidf.kvstore;

<<<<<<< HEAD
=======
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.deeplearning4j.models.word2vec.Word2Vec;
import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.db.Serializers;
import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.tfidf.VectorSerialiser;

>>>>>>> refs/remotes/origin/FileWalker
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.db.Serializers;
import uk.ac.cam.cl.charlie.math.Vector;

/**
 * Class for managing word2vec database
 *
 * @author Shyam Tailor
 * @author Gary Guo
 */
public final class WordVecDB {
    private static WordVecDB instance;

    private Cache<String, Optional<Vector>> cache;

    private Database db;
    PersistentMap<String, Vector> map;
    String vectorDBName = "vectors";
<<<<<<< HEAD

    private class VectorSerialiser extends Serializer<Vector> {
        @Override
        public boolean typecheck(Object obj) {
            return obj instanceof Vector;
        }

        @Override
        public byte[] serialize(Vector object) {
            ByteBuffer buf = ByteBuffer.allocate(object.size() * 8);

            for (int i = 0; i < object.size(); ++i) {
                buf.putDouble(object.get(i));
            }
            return buf.array();
        }

        @Override
        public Vector deserialize(byte[] bytes) {
            if (bytes.length % 8 != 0) {
                throw new IllegalArgumentException();
            }

            ByteBuffer buf = ByteBuffer.wrap(bytes);

            double[] array = new double[bytes.length / 8];

            for (int i = 0; i < array.length; ++i) {
                array[i] = buf.getDouble();
            }

            return new Vector(array);
        }
    }
=======
>>>>>>> refs/remotes/origin/FileWalker

    private WordVecDB() {
        cache = CacheBuilder.newBuilder().maximumSize(2000).build();
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

        try (BufferedReader br = new BufferedReader(new FileReader(vectorFile));
                PersistentMap<String, Vector>.BatchWriter batch = db.map.batch()) {
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
                batch.put(word, new Vector(components));
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

}
