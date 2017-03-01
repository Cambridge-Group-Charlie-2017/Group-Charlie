package uk.ac.cam.cl.charlie.filewalker;

import uk.ac.cam.cl.charlie.db.PersistentSet;

import java.nio.file.Path;

/**
 * Created by shyam on 26/02/2017.
 * This class is just the basic file walker except it remembers which directories are being listened to
 * on a reboot
 */
public class PersistentFileWalker extends BasicFileWalker {

    private PersistentSet<Path> persistentRootDirs;

    public PersistentFileWalker() {
        super();
        restoreFromDB();
    }

    private void restoreFromDB() {
        for (Path p : persistentRootDirs) {
            super.addRootDirectory(p);
        }
    }

    @Override
    public void addRootDirectory(Path p) {
        super.addRootDirectory(p);
        persistentRootDirs.add(p);
    }

    @Override
    public void removeRootDirectory(Path p) {
        super.removeRootDirectory(p);
        persistentRootDirs.remove(p);
    }
}
