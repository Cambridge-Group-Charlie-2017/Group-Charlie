package uk.ac.cam.cl.charlie.clustering;

import org.junit.Test;

import javax.mail.Message;
import java.io.File;
import java.util.ArrayList;

/**
 * @author Matthew Boyce
 */
public class ClusterNamerTest {

    @Test public void senderNameTest1() throws Exception{
        ArrayList<Message> messages= new ArrayList<Message>();
        ArrayList<File> files = new ArrayList<File>();
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));

        KMeansCluster c = new KMeansCluster(null,messages);
        ClusterNamer.senderNaming(c);
        assert(c.getName().equals("companyname"));
    }

    @Test public void senderNameTest2(){
        ArrayList<Message> messages= new ArrayList<Message>();
        ArrayList<File> files = new ArrayList<File>();
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@a-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@b-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@c-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@d-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@e-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@f-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@g-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@h-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@i-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@j-companyname.com","Project X", "",files));

        KMeansCluster c = new KMeansCluster(null,messages);

        try {
            ClusterNamer.senderNaming(c);
            //Should never reach as should throw error
            assert (false);
        } catch (ClusterNamingException e) {
            //Should reach this if task passes
        }
    }

    @Test public void nameTest(){
        ArrayList<Message> messages= new ArrayList<Message>();
        ArrayList<File> files = new ArrayList<File>();
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@a-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@b-companyname.com","Project X another", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@c-companyname.com","Project X request", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@d-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@e-companyname.com","Project X blaa", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@f-companyname.com","Project X ", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@g-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@h-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@i-companyname.com","Project X", "",files));
        messages.add(MessageCreator.createMessage("blabla@gmail.com","Bob@j-companyname.com","Project X", "",files));

        KMeansCluster c = new KMeansCluster(null,messages);

        ClusterNamer.name(c);
        System.out.println(c.getName());
        assert (c.getName().equals("Project X"));
    }

}
