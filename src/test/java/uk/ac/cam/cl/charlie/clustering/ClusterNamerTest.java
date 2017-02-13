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
        ArrayList<ClusterableObject> messages= new ArrayList<ClusterableObject>();
        ArrayList<File> files = new ArrayList<File>();
        for (int i = 0; i < 250; i++)
            messages.add(new ClusterableMessage(MessageCreator.createMessage(
               "blabla@gmail.com","Bob@companyname.com","Project X", "A",files)));
        GenericDummyVectoriser.train(messages);

        GenericEMCluster c = new GenericEMCluster(messages);
        ClusterNamer.senderNaming(c);
        assert(c.getName().equals("companyname"));
    }

    @Test public void senderNameTest2(){
        ArrayList<ClusterableObject> messages= new ArrayList<ClusterableObject>();
        ArrayList<File> files = new ArrayList<File>();
        for (int i = 0; i < 250; i++)
            messages.add(new ClusterableMessage(MessageCreator.createMessage(
               "blabla@gmail.com","Bob@"+i+"-companyname.com","Project X", "",files)));

        GenericDummyVectoriser.train(messages);

        GenericEMCluster c = new GenericEMCluster(messages);

        try {
            ClusterNamer.senderNaming(c);
            //Should never reach as should throw error
            assert (false);
        } catch (ClusterNamingException e) {
            //Should reach this if task passes
        }
    }

    @Test public void subjectNameTest() throws ClusterNamingException {
        ArrayList<ClusterableObject> messages= new ArrayList<ClusterableObject>();
        ArrayList<File> files = new ArrayList<File>();
        for(int i =0; i <25; i++) {
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@a-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@b-companyname.com", "Project X another", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@c-companyname.com", "Project X request", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@d-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@e-companyname.com", "Project X blaa", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@f-companyname.com", "Project X ", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@g-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@h-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@i-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@j-companyname.com", "Project X", "A", files)));
        }

        GenericDummyVectoriser.train(messages);

        GenericEMCluster c = new GenericEMCluster(messages);

        ClusterNamer.subjectNaming(c);
        System.out.println(c.getName());
        assert (c.getName().equals("Project X "));
    }

    @Test public void nameTest(){
        ArrayList<ClusterableObject> messages= new ArrayList<ClusterableObject>();
        ArrayList<File> files = new ArrayList<File>();
        for(int i=0; i<25;i++) {
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@a-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@b-companyname.com", "Project X another", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@c-companyname.com", "Project X request", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@d-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@e-companyname.com", "Project X blaa", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@f-companyname.com", "Project X ", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@g-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@h-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@i-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com", "Bob@j-companyname.com", "Project X", "A", files)));
        }
        GenericDummyVectoriser.train(messages);

        GenericEMCluster c = new GenericEMCluster(messages);

        ClusterNamer.name(c);
        System.out.println(c.getName());
        assert (c.getName().equals("Project X"));
    }

}
