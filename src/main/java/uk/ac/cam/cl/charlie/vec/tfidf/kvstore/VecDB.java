package uk.ac.cam.cl.charlie.vec.tfidf.kvstore;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.util.OS;

import java.io.*;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Created by shyam on 15/02/2017.
 */
public final class VecDB implements Closeable {
    private static VecDB instance;
    private static final String dbLocation = OS.getAppDataDirectory() + "vectors.db";
    private static final String mapName = "vectors";

    private Cache<String, Vector> cache;
    private DB database;
    private BTreeMap<String, double[]> map;

    private VecDB() {
        open();
        cache = CacheBuilder.newBuilder()
                .maximumSize(2000)
                .build();
    }

    public void open() {
        if (database != null && !database.isClosed()) {
            return;
        }
        File f = new File(dbLocation);
        database = DBMaker
                .fileDB(f)
                .fileMmapEnableIfSupported()
                .transactionEnable()
                .closeOnJvmShutdown()
                .make();

        map = database.treeMap(mapName)
              .keySerializer(Serializer.STRING)
              .valueSerializer(Serializer.DOUBLE_ARRAY)
              .createOrOpen();
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

}
