package uk.ac.cam.cl.charlie.filewalker;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Created by shyam on 20/02/2017.
 */
public final class FileDB {
    private static FileDB instance;

    private Database db;
    private Map<Path, Vector> priorityMap; // this one is todo
    private Map<Path, Vector> fullMap;

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
        // todo
        // note that attr includes an object "key" that uniquely identifies a file; maybe useful?
        // todo
        if (isReadableFile(p)) {
            if (!vectoriser.minimumBatchSizeReached()) {
                // just add to a queue that will eventually be flushed
                vectorisingQueue.add(p);
            }

            else {
                if (vectorisingQueue.size() != 0) {
                    // haven't flushed the queue yet - this is probably the first opportunity
                    vectorisingQueue.add(p);
                    vectoriser.documentBatch2vec(vectorisingQueue);
                }
            }
        }


    }

    public void processDeletedFile(Path p) {
        fullMap.remove(p);
    }

    public void processModifiedFile(Path p, BasicFileAttributes attrs) {
        // todo
        fullMap.put(p, Vector.zero(300));
    }

    private static boolean isReadableFile(Path p) throws IOException {
        String mimeType = Files.probeContentType(p);
        return mimeType.equals("text/plain");
        // can worry about pdfs and others later
    }

    private Optional<Vector> vectoriseFileIfPossible(Path p) throws IOException, BatchSizeTooSmallException {
        if (isReadableFile(p)) {
            String contents = new String(Files.readAllBytes(p));
            String filename = p.getFileName().toString();
            Document doc = new Document(filename, contents);

            return Optional.of(vectoriser.emailBatch2vec(doc));
        } else {
            return Optional.empty();
        }
    }

}
