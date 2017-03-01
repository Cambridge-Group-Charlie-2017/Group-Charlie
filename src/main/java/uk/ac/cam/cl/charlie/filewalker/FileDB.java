package uk.ac.cam.cl.charlie.filewalker;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.Document;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;
import uk.ac.cam.cl.charlie.vec.tfidf.kvstore.VectorSerialiser;

/**
 * Created by shyam on 20/02/2017.
 */
public final class FileDB {
    private static FileDB instance;

    private Database db;

    /* Not needed anymore if we agree on using a HashMap for priorityFiles
    private class PathVectorPair {
        public Path p;
        public Vector v;

        public PathVectorPair(Path p, Vector v) {
            this.p = p;
            this.v = v;
        }
    }*/

    private Map<Path, Vector> priorityFiles;
    private Map<Path, Vector> fullMap;

    private static Logger log = LoggerFactory.getLogger(FileDB.class);

    private List<Document> vectorisingQueue;

    private VectorisingStrategy vectoriser;

    private static final int prioritySize = 100;
    private static final double toleranceForSimilarity = 0.8;

    private FileDB() {
        fullMap = Database.getInstance().getMap("files", new PathSerialiser(), new VectorSerialiser());
        vectoriser = TfidfVectoriser.getVectoriser();
        vectorisingQueue = new LinkedList<>();
        priorityFiles = new HashMap<>(prioritySize);
    }

    public static FileDB getInstance() {
        if (instance == null) {
            instance = new FileDB();
        }
        return instance;
    }


    public void processNewFile(Path p, BasicFileAttributes attrs) {
        processFile(p);
    }

    private void processFile(Path p) {
        // for now - just an auxilliary method; there is no difference between a modified and a new
        // file really so far
        try {
            if (FileReader.isReadableFile(p)) {
                if (!vectoriser.minimumBatchSizeReached()) {
                    // just add to a queue that will eventually be flushed
                    try {
                        Document d = FileReader.readFile(p);
                        vectorisingQueue.add(d);
                    } catch (IOException | UnreadableFileTypeException e) {
                        log.debug("file apparently no longer readable or accessible", e);
                        return;
                        // just ignore it - if the file is now magically changed just return, and ignore it
                    }
                } else {
                	Document d = FileReader.readFile(p); 
                    if (vectorisingQueue.size() != 0) {
                        // haven't flushed the queue yet - this is probably the first opportunity
                        vectorisingQueue.add(d);
                       List<Vector> vectors = vectoriser.batchdoc2vec(vectorisingQueue);
                        if (vectors.size() != vectorisingQueue.size()) {
                            throw new Error("Assertion failure - the length of the list of vectors should match" +
                                    "the length of the list of documents");
                        }

                        Iterator<Document> documentIterator = vectorisingQueue.iterator();
                        Iterator<Vector> vectorIterator = vectors.iterator();

                        while (documentIterator.hasNext()) {
                            Path path = documentIterator.next().getPath();
                            Vector v = vectorIterator.next();
                            fullMap.put(path, v);
                            putIntoPriority(path, v);
                        }

                        vectorisingQueue.clear();
                    }

                    else {
                        Path path = d.getPath();
                        Vector v = vectoriser.doc2vec(d);
                        fullMap.put(path,v);
                        putIntoPriority(path, v);
                    }
                }
            }
        } catch (IOException | UnreadableFileTypeException e) {
            log.debug("file apparently no longer readable or accessible", e);
            return; // can't just die because a file is no longer readable or has magically changed
            // just ignore it! Missing a file is not hugely important
        } 

    }

    public void processDeletedFile(Path p) {
        fullMap.remove(p);
        priorityFiles.remove(p);
    }

    public void processModifiedFile(Path p, BasicFileAttributes attrs) {
        processFile(p);
    }


    public Vector getVector(Path p) {//note that the path might not exist or there were not enough files yet to vectorise
    	return fullMap.get(p);
    }
    
    public Set<Path> getPriorityFiles() {
    	Set<Path> paths = new HashSet<Path>(prioritySize);
    	for(Path p: priorityFiles.keySet()) {
    		paths.add(p);
    	}
    	return paths;
    }
    
    public Optional<Path> getMostRelevantFile(Vector v) {
        // query priority first
        if (priorityFiles.size() != 0) {
            Optional<Path> prioritySuggestion = getBestMatch(v, priorityFiles);
            Optional<Path> result = prioritySuggestion.isPresent() ? prioritySuggestion : getBestMatch(v, fullMap);
            return result;
        }
        else {
            return getBestMatch(v, fullMap);
        }
    }
    
    private void putIntoPriority(Path p, Vector v) {
        if (!(priorityFiles.size() < prioritySize)) {
            priorityFiles.remove(prioritySize - 1);
        }
        priorityFiles.put(p,v);
    }

    private static Optional<Path> getBestMatch(Vector v, Map<Path, Vector> map) {
        Path min = null;
        double dotProd = 0.0; // dot product == cosine distance assuming normalisation
        for (Map.Entry<Path, Vector> entry : map.entrySet()) {
            double latestCosine = Math.abs(entry.getValue().dot(v));
            if (min == null || latestCosine > dotProd) { // i think it's greater than?
                min = entry.getKey();
                dotProd = latestCosine;
            }
        }
        if (dotProd > toleranceForSimilarity) {
            return Optional.of(min);
        }
        else {
            return Optional.empty();
        }
    }


    /* Not needed anymore
    private static Optional<Path> getBestMatch (Vector v, Set<PathVectorPair> s) {
>>>>>>> refs/heads/mailbox
        Path min = null;
        double dotProd = 0.0;

        for (PathVectorPair pvp : s) {
            double latestCosine = Math.abs(pvp.v.dot(v));
            if (min == null || latestCosine > dotProd) {
                min = pvp.p;
                dotProd = latestCosine;
            }
        }
<<<<<<< HEAD
        if (dotProd > toleranceForSimilarity) {
            return Optional.of(min);
        }
        else {
            return Optional.empty();
        }
    }
=======

        return min;
    }*/
    
}
