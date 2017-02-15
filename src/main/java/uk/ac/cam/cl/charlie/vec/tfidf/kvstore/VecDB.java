package uk.ac.cam.cl.charlie.vec.tfidf.kvstore;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.fusesource.leveldbjni.JniDBFactory;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.WriteOptions;
import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.util.OS;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Created by shyam on 15/02/2017.
 */
public final class VecDB {
    private static VecDB instance;
    private static final File dbLocation = new File(OS.getAppDataDirectory() + "vectors");

    private Cache<String, Vector> cache;

    // todo consider caching options

    private VecDB() {
        createBlankDBIfNeeded();
        cache = CacheBuilder.newBuilder()
                .maximumSize(2000)
                .build();
    }

    private void createBlankDBIfNeeded() {
        // following setup code given on repo
        Options options = new Options();
        options.createIfMissing(true);
        DB db = null;

        try {
            // open and shut to create db and check that everything is working as expected
            db = JniDBFactory.factory.open(dbLocation, options);
        } catch (IOException e) {
            throw new Error("Couldn't get initial access to the db", e);
        } finally {
            try {
                if (db != null)
                    db.close();
            } catch (IOException e) {
                throw new Error("Couldn't close the db", e);
            }
        }
    }

    public VecDB getInstance() {
        if (instance == null) {
            instance = new VecDB();
        }
        return instance;
    }

    public void eraseDB() {
        try {
            JniDBFactory.factory.destroy(dbLocation, new Options());
            createBlankDBIfNeeded();

        } catch (IOException e) {
            throw new Error("Couldn't destroy db", e);
        }
    }

    public void put(String w, Vector v) {
        try (DB db = JniDBFactory.factory.open(dbLocation, new Options())) {
            cache.invalidate(w);
            db.put(w.getBytes(), v.getBytes());
        } catch (IOException e) {
            throw new Error("Unable to open db for put", e);
        }
    }

    public Vector get(String w) {
        try (DB db = JniDBFactory.factory.open(dbLocation, new Options())) {
            try {
                return cache.get(w, new Callable<Vector>() {
                    @Override
                    public Vector call() throws Exception {
                        return Vector.fromBytes(db.get(w.getBytes()));
                    }
                });
            } catch (ExecutionException e) {
                throw new Error(e);
            }
        } catch (IOException e) {
            throw new Error("Unable to open db for put", e);
        }
    }

    public void delete(String w) {
        try (DB db = JniDBFactory.factory.open(dbLocation, new Options())) {
            cache.invalidate(w);
            db.delete(w.getBytes());
        } catch (IOException e) {
            throw new Error("Unable to open db for delete", e);
        }
    }

    public void writeBatch(List<WordVectorPair> pairs) {
        try (DB db = JniDBFactory.factory.open(dbLocation, new Options())) {
            WriteBatch wb = db.createWriteBatch();
            for (WordVectorPair p : pairs) {
                cache.invalidate(p.word);
                wb.put(p.word.getBytes(), p.v.getBytes());
            }
            db.write(wb);
        } catch (IOException e) {
            throw new Error("Unable to open db for batch put", e);
        }
    }

    public void deleteBatch(List<String> words) {
        try (DB db = JniDBFactory.factory.open(dbLocation, new Options())) {
            WriteBatch wb = db.createWriteBatch();
            cache.invalidateAll(words);
            for (String w : words) {
                wb.delete(w.getBytes());
            }
            db.write(wb);
        } catch (IOException e) {
            throw new Error("Unable to open db for batch delete", e);
        }
    }

    public List<WordVectorPair> getBatch(List<String> words) {
        try (DB db = JniDBFactory.factory.open(dbLocation, new Options())) {
            List<WordVectorPair> l = new LinkedList<>();
            for (String w : words) {
                try {
                    Vector v = cache.get(w, new Callable<Vector>() {
                        @Override
                        public Vector call() throws Exception {
                            return Vector.fromBytes(db.get(w.getBytes()));
                        }
                    });
                    l.add(new WordVectorPair(w, v));

                } catch (ExecutionException e) {
                    throw new Error(e);
                }
            }

            return l;
        } catch (IOException e) {
            throw new Error("Unable to open db for delete", e);
        }
    }
}
