package uk.ac.cam.cl.charlie.vec.tfidf;

import java.util.HashMap;
import java.util.TreeSet;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.vec.Document;
import uk.ac.cam.cl.charlie.vec.Vector;

/**
 * Created by shyam on 05/02/2017. Edited by LP 05/02/2017
 */
public final class Tfidf {
    // Presumably this will have to be stored in the database (which I will leave for later).
    // Use singleton pattern to stop subclassing

    private static Tfidf instance = null;
    private Database database; // the database instance should store the word frequencies.

    // TODO note I strongly disagree with this class having:
    // a) any tables (the data has to be persisted to a database
    // b) being used by external classes except TfidfVectoriser

    // TODO this implementation with the database

    // TODO I would also suggest aiming for very low coupling with the vectorising class
    // there are other uses for tfidf ;)

    /*
     * The following hash map is represents a table in which we denote the word frequency in a document.
     * A hash map is used as it is more efficient to add documents.
     * If a word is not in the hashmap for a doc, that means the word frequency = 0.
     */
    private HashMap<String, HashMap<String, Integer>> tfidfTableValues; // documents -> words -> frequency
    /*
     * The following set represents the order of words when we want to get the vector for a document from this table.
     */
    private TreeSet<String> words;
    
    private Tfidf() {
        tfidfTableValues = new HashMap<String, HashMap<String, Integer>>();
        words = new TreeSet<String>();
    }

    public static Tfidf getInstance() {
        if (instance == null) {
            instance = new Tfidf();
        }
        return instance;
    }

    public int totalNumberDocuments() {
        return tfidfTableValues.size();
    }
    
    public int numberOfDocsWithWith(String word) {
        int count = 0;
        for(HashMap<String, Integer> doc: tfidfTableValues.values()) {
        	if(doc.containsKey(word)) { // the word appears at least once in the document vs word frequency table
        		++count;
        	}
        }
        return count;
    }
    
    public void addDocument(Document doc) { //to be able to add documents to the documents vs words frequency table
    	HashMap<String, Integer> wordfrequency = new HashMap<String, Integer>();

        for (String w : doc.getContent().split("[\\W]")) {
            if(wordfrequency.containsKey(w)) {
            	int frequency = wordfrequency.get(w);
            	frequency++;
            	wordfrequency.put(w, frequency);
            } else { //word not yet in the map and set
            	wordfrequency.put(w, 1);
            	words.add(w);
            }
        }
        
        tfidfTableValues.put(doc.getName(),wordfrequency);
    }
    
    public void addDocument(Document doc, HashMap<String, Integer> wordfrequency) { // To be used when wordfrequency has already been calculated
    	tfidfTableValues.put(doc.getName(),wordfrequency);
    }

    public void giveDatabaseInstance(Database database) {
    	this.database = database;
    }

    public boolean hasDatabaseInstance() {
    	return database != null ? true : false;
    }

}
