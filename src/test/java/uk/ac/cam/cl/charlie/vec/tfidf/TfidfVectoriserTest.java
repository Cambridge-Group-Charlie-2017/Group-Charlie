package uk.ac.cam.cl.charlie.vec.tfidf;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class TfidfVectoriserTest {
	
	private TfidfVectoriser tfidf;

	public TfidfVectoriserTest() {
		tfidf = new TfidfVectoriser();
	}
	
	@Test
	public void testModelLoad() {
		tfidf.loadModel();
		assertEquals(true, tfidf.isModelLoaded());
	}
	
	@Test
	public void testModelisLoad() {
		tfidf.isModelLoaded();
	}
	
	@Test
	public void testpersistModel() {
		tfidf.persistModel();
	}
	
}
