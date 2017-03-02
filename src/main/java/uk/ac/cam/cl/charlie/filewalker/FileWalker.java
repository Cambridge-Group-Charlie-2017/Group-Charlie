package uk.ac.cam.cl.charlie.filewalker;

import java.nio.file.Path;
import java.util.List;

/**
 * Created by shyam on 20/02/2017.
 */
public interface FileWalker {
    public void addRootDirectory(Path p);

    public void removeRootDirectory(Path p);

    public List<Path> getRootDirectories();

    public void startWalkingTree();

    public void closeListener();

}
