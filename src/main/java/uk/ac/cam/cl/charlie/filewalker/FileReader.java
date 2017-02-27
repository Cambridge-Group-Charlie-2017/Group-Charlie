package uk.ac.cam.cl.charlie.filewalker;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import uk.ac.cam.cl.charlie.vec.Document;

/**
 * Created by shyam on 22/02/2017.
 */
public class FileReader {
    private static Tika tika = new Tika();
    private FileReader() {} // non-instantiable class

    private static Document readWithTika(Path p) throws IOException {
        String content = null;
        try {
            content = tika.parseToString(p);
        } catch (TikaException e) {
            throw new IOException(e); // for our purposes the distinction doesn't really matter
        }
        return new Document(p.toAbsolutePath(), content);
    }

    public static boolean isReadableFile(Path p) throws IOException {
        String mimeType = tika.detect(p.toString()); // to string forces it not to look inside the file
        // this is just quicker and prevents any locks on files
        if(mimeType == null) {
            return false;
        }

        // tika can parse a lot of files we don't want to pass - e.g. i doubt extracting text from
        // .rar files is going to help us
        switch (mimeType) {
            case "text/html":
                return true;
            case "application/vnd.apple.iwork":
                return true;
            case "application/vnd.apple.pages":
                return true;
            case "application/msword":
                return true;
            case "application/x-mspublisher":
                return true;
            case "application/vnd.ms-powerpoint":
                return true;
            case "application/pdf":
                return true;
            case "text/plain":
                return true;
            case "application/rtf":
                return true;
            default:
                return false;
        }
        // think I got the major ones
         
    }

    public static Document readFile(Path p) throws IOException, UnreadableFileTypeException {
        return readWithTika(p);
    }
}