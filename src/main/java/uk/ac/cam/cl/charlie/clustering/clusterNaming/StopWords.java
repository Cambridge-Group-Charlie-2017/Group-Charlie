package uk.ac.cam.cl.charlie.clustering.clusterNaming;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;

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
            if (stopWords == null)
                stopWords = IOUtils.readLines(new FileInputStream("stopwords.txt"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return stopWords;
    }

}
