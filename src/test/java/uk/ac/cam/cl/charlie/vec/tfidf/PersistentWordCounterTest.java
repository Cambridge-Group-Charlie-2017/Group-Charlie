package uk.ac.cam.cl.charlie.vec.tfidf;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test for class {@link PersistentWordCounter}
 *
 * @author Shyam Tailor, Gary Guo
 *
 */
public class PersistentWordCounterTest {
    private PersistentWordCounter counter;

    public PersistentWordCounterTest() {
	counter = PersistentWordCounter.getInstance();
	wipe();
    }

    private void wipe() {
	// counter.map.clear();
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
