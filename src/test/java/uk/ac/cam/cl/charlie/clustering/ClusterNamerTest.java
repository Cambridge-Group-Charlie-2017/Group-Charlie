package uk.ac.cam.cl.charlie.clustering;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Vector;

import javax.mail.Message;

import org.junit.Test;

import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;

import uk.ac.cam.cl.charlie.clustering.clusterNaming.ClusterNamer;
import uk.ac.cam.cl.charlie.clustering.clusterNaming.ClusterNamingException;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;
import uk.ac.cam.cl.charlie.clustering.clusters.EMCluster;

/**
 * @author Matthew Boyce
 */
public class ClusterNamerTest {

    @Test
    public void senderNameTest1() throws Exception {
        ArrayList<ClusterableObject<Message>> messages = new ArrayList<>();
        ArrayList<File> files = new ArrayList<>();
        for (int i = 0; i < 250; i++)
            messages.add(new ClusterableMessage(
                    MessageCreator.createMessage("blabla@gmail.com", "Bob@companyname.com", "Project X", "A", files)));

        Cluster<Message> c = new EMCluster<>(messages);
        ClusterNamer.senderNaming(c);
        assert (c.getName().equals("companyname"));
    }

    @Test
    public void senderNameTest2() {
        ArrayList<ClusterableObject<Message>> messages = new ArrayList<>();
        ArrayList<File> files = new ArrayList<>();
        for (int i = 0; i < 250; i++)
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@" + i + "-companyname.com", "Project X", "", files)));

        EMCluster<Message> c = new EMCluster<>(messages);

        try {
            ClusterNamer.senderNaming(c);
            // Should never reach as should throw error
            assert (false);
        } catch (ClusterNamingException e) {
            // Should reach this if task passes
        }
    }

    @Test
    public void subjectNameTest() throws ClusterNamingException {
        ArrayList<ClusterableObject<Message>> messages = new ArrayList<>();
        ArrayList<File> files = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@a-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@b-companyname.com", "Project X another", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@c-companyname.com", "Project X request", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@d-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@e-companyname.com", "Project X blaa", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@f-companyname.com", "Project X ", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@g-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@h-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@i-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@j-companyname.com", "Project X", "A", files)));
        }

        EMCluster<Message> c = new EMCluster<>(messages);

        ClusterNamer.subjectNaming(c);
        System.out.println(c.getName());
        assert (c.getName().equals("Project X "));
    }

    @Test
    public void nameTest() throws Exception {
        ArrayList<ClusterableObject<Message>> messages = new ArrayList<>();
        ArrayList<File> files = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@a-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@b-companyname.com", "Project X another", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@c-companyname.com", "Project X request", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@d-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@e-companyname.com", "Project X blaa", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@f-companyname.com", "Project X ", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@g-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@h-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@i-companyname.com", "Project X", "A", files)));
            messages.add(new ClusterableMessage(MessageCreator.createMessage("blabla@gmail.com",
                    "Bob@j-companyname.com", "Project X", "A", files)));
        }

        EMCluster<Message> c = new EMCluster<>(messages);

        ClusterNamer.name(c);
        System.out.println(c.getName());
        assert (c.getName().equals("Project X "));
    }

    @Test
    public void nameTest2() throws Exception {
        String filename = "src/main/resources/enron/bass-e.pst";
        Date initial = new Date();
        PSTFile pstFile = new PSTFile(filename);
        ArrayList<ClusterableMessage> clusterableMessages = processFolder(pstFile.getRootFolder());
        System.out.println("Loaded messages into memory");
        Date loaded = new Date();

        EMClusterer<Message> clusterer = new EMClusterer<>(clusterableMessages);
        ClusterGroup<Message> clusters = clusterer.getClusters();
        System.out.println("emails clustered");
        Date clustered = new Date();

        for (int i = 0; i < clusters.size(); i++) {
            System.out.println("Cluster Name: " + ClusterNamer.name(clusters.get(i)) + "\n");
            for (int j = 0; j < clusters.get(i).getObjects().size(); j++)
                System.out.println(clusters.get(i).getObjects().get(j).getObject().getSubject() + "\n");
            System.out.println("\n\n\n");
        }

        for (int i = 0; i < clusters.size(); i++) {
            System.out.println("Cluster Name: " + clusters.get(i).getName() + "\n");
        }

        Date named = new Date();

        System.out.println("Loading stage: " + (loaded.getTime() - initial.getTime()) + "ms");
        System.out.println("Clustering stage: " + (clustered.getTime() - loaded.getTime()) + "ms");
        System.out.println("Loading stage: " + (named.getTime() - clustered.getTime()) + "ms");
    }

    @Test
    public void clusteringTest() throws Exception {
        String filename = "src/main/resources/enron/bass-e.pst";
        PSTFile pstFile = new PSTFile(filename);
        ArrayList<ClusterableMessage> clusterableMessages = processFolder(pstFile.getRootFolder());

        EMClusterer<Message> clusterer = new EMClusterer<>(clusterableMessages);
        ClusterGroup<Message> clusters = clusterer.getClusters();
        for (int i = 0; i < clusters.size(); i++) {
            System.out.println("Cluster Name: " + ClusterNamer.name(clusters.get(i)) + "\n");
            for (int j = 0; j < clusters.get(i).getObjects().size(); j++)
                System.out.println(clusters.get(i).getObjects().get(j).getObject().getSubject() + "\n");
            System.out.println("\n\n\n");
        }

        for (int i = 0; i < clusters.size(); i++) {
            System.out.println("Cluster Name: " + clusters.get(i).getName() + "\n");
        }
    }

    public ArrayList<ClusterableMessage> processFolder(final PSTFolder folder)
            throws PSTException, java.io.IOException {
        ArrayList<ClusterableMessage> messages = new ArrayList<>();

        if (folder.hasSubfolders()) {
            final Vector<PSTFolder> childFolders = folder.getSubFolders();
            for (final PSTFolder childFolder : childFolders) {
                messages.addAll(this.processFolder(childFolder));
            }
        }
        int count = 0;
        // and now the emails for this folder
        if (folder.getContentCount() > 0) {
            PSTMessage email = (PSTMessage) folder.getNextChild();
            while (email != null && count < 250) {
                // System.out.println("Email: " + email.getDescriptorNodeId() +
                // " - " + email.getSubject());
                ClusterableMessage clusterableMessage = new ClusterableMessage(MessageCreator.createMessage("a@b.com",
                        email.getSenderEmailAddress(), email.getSubject(), email.getBody(), new ArrayList<File>()));
                messages.add(clusterableMessage);
                email = (PSTMessage) folder.getNextChild();
                count++;
            }
        }
        return messages;
    }
}
