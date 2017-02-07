package uk.ac.cam.cl.charlie.vec.tfidf;

import org.junit.Test;

public class TfidfVectoriserTest {
	
	private TfidfVectoriser tfidf;

	public TfidfVectoriserTest() {
		tfidf = new TfidfVectoriser();
	}
	
	@Test
	public void testModelLoad() {
		tfidf.loadModel();
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
