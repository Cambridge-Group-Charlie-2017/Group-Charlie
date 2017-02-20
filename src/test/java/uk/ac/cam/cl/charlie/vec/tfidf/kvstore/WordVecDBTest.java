package uk.ac.cam.cl.charlie.vec.tfidf.kvstore;

import org.junit.Test;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.math.Vector;

import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Created by shyam on 19/02/2017.
 */
public class WordVecDBTest {
    private final String textPath = "src/main/resources/word2vec/wordvectors.txt";
    private WordVecDB db;

    public WordVecDBTest() {
        WordVecDB.populateFromTextFile(textPath);
        db = WordVecDB.getInstance();
    }

    @Test
    public void checkGet() {
        assertNotEquals(db.get("in"), Optional.of(Vector.zero(300)));
    }

    @Test
    public void checkPut() {
        Optional<Vector> v = db.get("the");
        assertNotEquals(Optional.of(Vector.zero(300)), db.get("the")) ;
        db.put("the", Vector.zero(300));
        assertEquals(Optional.of(Vector.zero(300)), db.get("the"));
        db.put("the", v.get());
    }

    @Test
    public void checkDelete() {
        Optional<Vector> v = db.get("land");
        assertNotEquals(Optional.empty(), db.get("land"));
        db.delete("land");
        assertEquals(Optional.empty(), db.get("land"));
        db.put("land", v.get());
    }

}
