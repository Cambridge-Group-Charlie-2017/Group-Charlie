package uk.ac.cam.cl.charlie.filewalker;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.stream.Stream.concat;

/**
 * Created by shyam on 20/02/2017.
 */
public class BasicFileWalker implements FileWalker {
    private Set<Path> rootDirs;
    private FileDB db;
    private WatchService watcher;

    public BasicFileWalker(Path root) {
        db = FileDB.getInstance();
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new Error(e);
        }

        rootDirs = new HashSet<>();
        rootDirs.add(root);

        // set off a background thread to process changes that are noticed during listening
        BackgroundChangeListener backgroundListener = new BackgroundChangeListener();
        backgroundListener.run();
    }

    @Override
    public void addRootDirectory(Path p) {
        rootDirs.add(p);
        walk(p);
    }

    @Override
    public void removeRootDirectory(Path p) {
        rootDirs.remove(p);
        // not obvious how you remove listening to a directory
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
                    dir.register(watcher, ENTRY_DELETE, ENTRY_MODIFY, ENTRY_CREATE);
                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new Error(e);
        }
    }

    private void addToListen(Path root) {
        try {
            // also need to walk down the tree and register any sub directories
            root.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeFromListen(Path root) {
        // this is not entirely obvious
    }


    private class BackgroundChangeListener implements Runnable {
        @Override
        public void run() {
            // see: https://docs.oracle.com/javase/tutorial/essential/io/notification.html
            // basically copied directly from this example

            while (true) {
                WatchKey key = null;
                try {
                    key = watcher.take(); // blocking
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
                    WatchEvent<Path> ev = (WatchEvent<Path>) event;
                    Path filename = ev.context();

                    // decide what to do with the file event

                    if (kind == ENTRY_DELETE) {
                        // get the db to process the deletion
                        db.processDeletedFile(filename);
                    }

                    else {
                        BasicFileAttributes attrs = null;
                        try {
                            attrs = Files.readAttributes(filename, BasicFileAttributes.class);
                        } catch (IOException e) {
                            throw new Error(e);
                        }

                        if (kind == ENTRY_CREATE) {
                            // add a new file to the db
                            db.processNewFile(filename, attrs);
                        }
                        else if (kind == ENTRY_MODIFY) {
                            // modify an existing file - the hard case
                            db.processModifiedFile(filename, attrs);
                        }
                    }
                }

                // have to reset the key to receive further events
                if(!key.reset()) {
                    // path deleted
                    key.cancel();
                }

            }
        }
    }
}
