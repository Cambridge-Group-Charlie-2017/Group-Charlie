package uk.ac.cam.cl.charlie.clustering;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * @author M Boyce
 */
public class StopWords {

    private static List<String> stopWords;

    private StopWords() {
    }

    @SuppressWarnings("unchecked")
    public static List<String> getStopWords() {

        try {
            if(stopWords == null)
                stopWords =  IOUtils.readLines(new FileInputStream("stopwords.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stopWords;
    }

}
