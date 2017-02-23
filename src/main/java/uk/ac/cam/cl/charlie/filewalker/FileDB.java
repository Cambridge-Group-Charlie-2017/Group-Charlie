package uk.ac.cam.cl.charlie.filewalker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.BatchSizeTooSmallException;
import uk.ac.cam.cl.charlie.vec.Document;
import uk.ac.cam.cl.charlie.vec.VectorisingStrategy;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;
import uk.ac.cam.cl.charlie.vec.tfidf.VectorSerialiser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

/**
 * Created by shyam on 20/02/2017.
 */
public final class FileDB {
    private static FileDB instance;

    private Database db;
    private Map<Path, Vector> priorityMap; // this one is todo
    private Map<Path, Vector> fullMap;

    private static Logger log = LoggerFactory.getLogger(FileDB.class);

    private List<Document> vectorisingQueue;

    private VectorisingStrategy vectoriser;

    private static final int prioritySize = 100;

    private FileDB() {
        // todo
        db = Database.getInstance();
        fullMap = db.getMap("files", new PathSerialiser(), new VectorSerialiser());
        vectoriser = TfidfVectoriser.getVectoriser();
        vectorisingQueue = new LinkedList<>();
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
                        List<Vector> vectors = vectoriser.documentBatch2vec(vectorisingQueue);
                        if (vectors.size() != vectorisingQueue.size()) {
                            throw new Error("Assertion failure - the length of the list of vectors should match" +
                                    "the length of the list of documents");
                        }

                        Iterator<Document> documentIterator = vectorisingQueue.iterator();
                        Iterator<Vector> vectorIterator = vectors.iterator();

                        while (documentIterator.hasNext()) {
                            fullMap.put(documentIterator.next().getPath().toAbsolutePath(), vectorIterator.next());
                        }

                        vectorisingQueue.clear();
                    }

                    else {
                        fullMap.put(d.getPath().toAbsolutePath(),vectoriser.doc2vec(d));
                    }
                }
            }
        } catch (IOException | UnreadableFileTypeException |
                BatchSizeTooSmallException e) {
            log.debug("file apparently no longer readable or accessible", e);
            return; // can't just die because a file is no longer readable or has magically changed
            // just ignore it! Missing a file is not hugely important
        }

    }

    public void processDeletedFile(Path p) {
        fullMap.remove(p);
    }

    public void processModifiedFile(Path p, BasicFileAttributes attrs) {
        // todo
        processFile(p);
    }

    private Path getMostRelevantFile(Vector v) {
        // query priority first
        return null; // todo
    }

}
