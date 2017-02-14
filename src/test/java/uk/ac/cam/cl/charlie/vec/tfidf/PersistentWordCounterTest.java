package uk.ac.cam.cl.charlie.vec.tfidf;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import uk.ac.cam.cl.charlie.db.Database;

/**
 * Test for class {@link PersistentWordCounter}
 *
 * @author Shyam Tailor, Gary Guo
 *
 */
public class PersistentWordCounterTest {
    private PersistentWordCounter counter;

    public PersistentWordCounterTest() {
	wipe();
	counter = PersistentWordCounter.getInstance();
    }

    private void wipe() {
	Database d = Database.getInstance();
	if (d.tableExists("WORD_FREQUENCIES")) {
	    d.executeUpdate("DROP TABLE WORD_FREQUENCIES");
	}
    }

    @Test
    public void checkAddWord() {
	String testWord = "Hello";

	int count = counter.frequency(testWord);
	counter.increment(testWord);
	assertEquals(count + 1, counter.frequency(testWord));
	counter.increment(testWord, 3);
	assertEquals(count + 4, counter.frequency(testWord));
    }

    @Test
    public void checkTotalIdempotent() {
	String testWord = "Hello";

	assertEquals(counter.frequency(testWord), counter.frequency(testWord));
    }
}
