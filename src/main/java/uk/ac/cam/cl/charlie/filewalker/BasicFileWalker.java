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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.HashMap;

/**
 * Created by shyam on 20/02/2017.
 */
public class BasicFileWalker implements FileWalker {
    private Set<Path> rootDirs;
    private FileDB db;
    private WatchService watcher;
    private HashMap<Path,WatchKey> watchedDirectories;
    
    private volatile boolean stopExecution = false;

    public BasicFileWalker(Path root) {
    	db = FileDB.getInstance();
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            throw new Error(e);
        }

        watchedDirectories = new HashMap<>();
        rootDirs = new HashSet<>();
        rootDirs.add(root);
        System.out.println("rootDirs initialised.");
        // set off a background thread to process changes that are noticed during listening
        BackgroundChangeListener backgroundListener = new BackgroundChangeListener();
        Thread listener = new Thread(backgroundListener);
        listener.start();
    }
    
    public void closeListener() {
    	stopExecution = true;
    }

    @Override
    public void addRootDirectory(Path p) {
        p = p.toAbsolutePath();
        rootDirs.add(p);
        walk(p);
        addToListen(p); //* Should also add to listen. Is this the correct approach?
    }

    @Override
    public void removeRootDirectory(Path p) {
        p = p.toAbsolutePath();
        rootDirs.remove(p);
        removeFromListen(p); //* Assuming you want to make this call here
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
        root = root.toAbsolutePath();
        try {
            // also need to walk down the tree and register any sub directories //*TODO: Is this meant to be a TODO comment?
            WatchKey watched = root.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watchedDirectories.put(root, watched);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeFromListen(Path root) {
        //Call cancel() on the WatchKey object representing the directory 'root', remove from hashmap.
        root = root.toAbsolutePath();
        WatchKey watchKey = watchedDirectories.get(root);
        if (watchKey != null) {
            watchKey.cancel();
            watchedDirectories.remove(root);
        }

        //*TODO: Should this now traverse all subdirectories and do the same?
    }


    private class BackgroundChangeListener implements Runnable {
        @Override
        public void run() {
            // see: https://docs.oracle.com/javase/tutorial/essential/io/notification.html
            // basically copied directly from this example

            while (!stopExecution) {
                WatchKey key = null;
                try {
                    key = watcher.take(); // blocking
                    if(stopExecution) break;//execution was stopped event might be caused by clean up on folder structure
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
                    } else {
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
