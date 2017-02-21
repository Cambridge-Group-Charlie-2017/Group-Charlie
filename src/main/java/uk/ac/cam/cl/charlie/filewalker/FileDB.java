package uk.ac.cam.cl.charlie.filewalker;

import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Created by shyam on 20/02/2017.
 */
public final class FileDB {
    private static FileDB instance;

    private FileDB () {
        // todo
        return;
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
    }

    public void processDeletedFile(Path p) {
        // todo
    }

    public void processModifiedFile(Path p, BasicFileAttributes attrs) {
        // todo
    }
}
