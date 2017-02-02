import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Created by Ben on 01/02/2017.
 */
public class KMeansWrapperTest {
    //TODO: use pre-loaded vectors, output clusters.


    @Test public void mainTest() throws Exception{
        KMeansWrapper km = new KMeansWrapper();

        ArrayList<Vector<Double>> vecs = new ArrayList<Vector<Double>>();

        vecs = km.parseArff("iris-vector.arff");

        km.createArff(vecs, "tempFile.arff");
        //To be certain, check tempFile.arff and iris-vector.arff. Contents should be identical apart from string attributes.

        ArrayList<Cluster> clusters = km.run(vecs);
        assertNotNull(clusters);
    }



}
