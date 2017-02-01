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


    @Test public void main() throws IOException{
        KMeansWrapper km = new KMeansWrapper();

        Vector<Double> vec = new Vector<Double>();
        vec.add(1.0);
        vec.add(2.0);
        vec.add(3.0);
        vec.add(4.0);
        vec.add(5.0);

        Vector<Double> vec2 = new Vector<Double>();
        vec2.add(5.0);
        vec2.add(4.0);
        vec2.add(3.0);
        vec2.add(2.0);
        vec2.add(1.0);

        ArrayList<Vector<Double>> vecs = new ArrayList<Vector<Double>>();
        vecs.add(vec);
        vecs.add(vec2);

        km.createAarf(vecs, "tempFile.aarf");
        //check generated file for results.
    }
}
