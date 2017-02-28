import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;
import uk.ac.cam.cl.charlie.clustering.MessageCreator;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;

import javax.mail.Message;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by M Boyce on 28/02/2017.
 */
public class PSTProcessor {
    public ArrayList<Message> processAllEnron() throws PSTException, IOException {
        String filename = "src/main/resources/bass-e.pst";
        PSTFile pstFile = new PSTFile(filename);
        ArrayList<Message> messages = processFolder(pstFile.getRootFolder());
        return messages;
    }

    public ArrayList<Message> processFolder(final PSTFolder folder) throws PSTException, java.io.IOException {
        ArrayList<Message> messages = new ArrayList<Message>();


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
            while (email != null) {
                //System.out.println("Email: " + email.getDescriptorNodeId() + " - " + email.getSubject());
                ClusterableMessage clusterableMessage = new ClusterableMessage(MessageCreator.createMessage("a@b.com",
                        email.getSenderEmailAddress(),email.getSubject(),email.getBody(),new ArrayList<File>()));
                messages.add(clusterableMessage.getMessage());
                email = (PSTMessage) folder.getNextChild();
                count++;
            }
        }
        return messages;
    }
}
