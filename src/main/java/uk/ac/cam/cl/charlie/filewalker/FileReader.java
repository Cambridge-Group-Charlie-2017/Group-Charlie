package uk.ac.cam.cl.charlie.filewalker;

import uk.ac.cam.cl.charlie.vec.Document;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by shyam on 22/02/2017.
 */
public class FileReader {
    private FileReader() {} // non-instantiable class

    private static Document readPlainText(Path p) throws IOException {
        String contents = new String(Files.readAllBytes(p));
        String filename = p.getFileName().toString();
        return new Document(p.toAbsolutePath(), contents);
    }

    public static boolean isReadableFile(Path p) throws IOException {
        String mimeType = Files.probeContentType(p);
        if(mimeType == null) {
        	return false;
        } else if(mimeType.equals("text/plain")) {
        	return true;
        } else { // can worry about pdfs and others later
        	return false;
        }
         
    }

    public static Document readFile(Path p) throws IOException, UnreadableFileTypeException {
        String mimeType = Files.probeContentType(p);

        // missing breaks aren't needed because they aren't needed ;)
        switch (mimeType) {
            case "text/plain":
                return readPlainText(p);
            default:
                throw new UnreadableFileTypeException();
        }
    }
}