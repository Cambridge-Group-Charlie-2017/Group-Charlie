package uk.ac.cam.cl.charlie.vec.tfidf.kvstore;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.inmemory.InMemoryLookupTable;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.nd4j.linalg.factory.Nd4j;
import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.math.Vector;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Created by shyam on 15/02/2017.
 */
public final class VecDB implements Closeable {
    // todo: possibility of batch insertion into the db (documentation is a tad awful on this)
    private static VecDB instance;

    private Cache<String, Vector> cache;
    private Database db;

    private class StringSerializer extends Serializer<String> {

        @Override
        public boolean typecheck(Object obj) {
            return false;
        }

        @Override
        public byte[] serialize(String object) {
            return object.getBytes();
        }

        @Override
        public String deserialize(byte[] bytes) {
            return new String(bytes);
        }
    }

    private VecDB() {
        cache = CacheBuilder.newBuilder()
                .maximumSize(2000)
                .build();
    }

    public static VecDB getInstance() {
        if (instance == null) {
            instance = new VecDB();
        }
        return instance;
    }

    @Override
    public void close() {
        database.commit();
        map.close();
        database.close();
    }

    public boolean isClosed() {
        return database.isClosed();
    }

    private static void PopulateFromTextFile() {
        // todo testing
        VecDB db = VecDB.getInstance();
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
        cache.invalidate(w);
        map.put(w, v.toDoubleArray());
    }

    public Vector get(String w) {
        try {
            return cache.get(w, new Callable<Vector>() {
                @Override
                public Vector call() throws Exception {
                    return new Vector(map.get(w));
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

        Iterator<Map.Entry<String, double[]>> entries = map.entryIterator();

        while (entries.hasNext()) {
            Map.Entry<String, double[]> entry = entries.next();
            table.putVector(entry.getKey(), Nd4j.create(entry.getValue()));
        }

        // if this doesn't work there is an alternative way of doing it with the Builder class
        Word2Vec w = new Word2Vec();
        w.setLookupTable(table);
        return w;
    }

}
