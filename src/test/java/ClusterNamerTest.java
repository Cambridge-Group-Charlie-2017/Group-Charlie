import org.junit.Test;
import uk.ac.cam.cl.charlie.clustering.*;
import javax.mail.Message;
import java.util.ArrayList;

/**
 * @author Matthew Boyce
 */
public class ClusterNamerTest {

    @Test public void mainTest() throws Exception{
        ArrayList<Message> messages = MessageCreator.createTestMessages();
        KMeansCluster c = new KMeansCluster(null,messages);
        ClusterNamer.senderNaming(c);
        assert(c.getName().equals("companyname"));
    }

}
