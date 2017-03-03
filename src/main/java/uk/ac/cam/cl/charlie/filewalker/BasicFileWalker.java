package uk.ac.cam.cl.charlie.filewalker;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by shyam on 20/02/2017.
 */
public class BasicFileWalker implements FileWalker {
    private Set<Path> rootDirs;
    private FileDB db;
    private WatchService watcher;
    private HashMap<Path, WatchKey> watchedDirectories;

    private Thread daemon;

    private volatile boolean stopExecution = false;

    public BasicFileWalker() {
        db = FileDB.getInstance();
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new Error(e);
        }

        watchedDirectories = new HashMap<>();
        rootDirs = new HashSet<>();

        daemon = new Thread(this::run);
        daemon.setDaemon(true);
        daemon.start();
    }

    @Override
    public void closeListener() {
        stopExecution = true;
        daemon.interrupt();
        try {
            daemon.wait();
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void addRootDirectory(Path p) {
        p = p.toAbsolutePath();
        if (!rootDirs.contains(p)) {
            rootDirs.add(p);
            walk(p);
        }
    }

    @Override
    public void removeRootDirectory(Path p) {
        p = p.toAbsolutePath();
        rootDirs.remove(p);
        removeFromListen(p); // * Assuming you want to make this call here
    }

    @Override
    public List<Path> getRootDirectories() {
        List<Path> l = new ArrayList<>();
        l.addAll(rootDirs);
        return l;
    }

    @Override
    public void startWalkingTree() {
        for (Path root : rootDirs) {
            walk(root);
        }
    }

    private void walk(Path root) {
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    db.processNewFile(file, attrs);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    WatchKey key = dir.register(watcher, ENTRY_DELETE, ENTRY_MODIFY, ENTRY_CREATE);
                    watchedDirectories.put(dir, key);
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void removeFromListen(Path root) {
        // Call cancel() on the WatchKey object representing the directory
        // 'root', remove from hashmap.
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    db.processDeletedFile(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    dir = dir.toAbsolutePath();
                    WatchKey k = watchedDirectories.get(dir);
                    if (k != null) {
                        k.cancel();
                        watchedDirectories.remove(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void run() {
        // see:
        // https://docs.oracle.com/javase/tutorial/essential/io/notification.html
        // basically copied directly from this example

        while (!stopExecution) {
            WatchKey key = null;
            try {
                key = watcher.take(); // blocking
                if (stopExecution)
                    break;// execution was stopped event might be caused by
                          // clean up on folder structure
            } catch (InterruptedException e) {
                continue;
            }
            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();

                // have to check for overflow
                if (kind == OVERFLOW) {
                    continue;
                }
                // cast:
                Path dir = (Path) key.watchable();
                Path relfilename = (Path) event.context();
                Path fullFilename = dir.resolve(relfilename);

                // decide what to do with the file event

                if (kind == ENTRY_DELETE) {
                    // get the db to process the deletion
                    db.processDeletedFile(fullFilename);
                } else {
                    BasicFileAttributes attrs = null;
                    try {
                        attrs = Files.readAttributes(fullFilename, BasicFileAttributes.class);
                    } catch (IOException e) {
                        continue;
                    }

                    if (kind == ENTRY_CREATE) {
                        // add a new file to the db
                        db.processNewFile(fullFilename, attrs);
                    } else if (kind == ENTRY_MODIFY) {
                        // modify an existing file - the hard case
                        db.processModifiedFile(fullFilename, attrs);
                    }
                }
            }

            // have to reset the key to receive further events
            if (!key.reset()) {
                // path deleted
                key.cancel();
            }
        }
    }
}
