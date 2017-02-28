package uk.ac.cam.cl.charlie.demo;

import com.pff.PSTException;
import com.pff.PSTFile;
import com.pff.PSTFolder;
import com.pff.PSTMessage;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Created by M Boyce on 28/02/2017.
 */
public class PSTProcessor {
    public ArrayList<MimeMessage> processAllEnron() throws PSTException, IOException {
        File file = new File("src/main/resources/enron/bass-e.pst");
        PSTFile pstFile = new PSTFile(file);
        return processFolder(pstFile.getRootFolder());
    }

    public ArrayList<MimeMessage> processFolder(final PSTFolder folder) throws PSTException, java.io.IOException {
        ArrayList<MimeMessage> messages = new ArrayList<>();


        if (folder.hasSubfolders()) {
            final Vector<PSTFolder> childFolders = folder.getSubFolders();
            for (final PSTFolder childFolder : childFolders) {
                messages.addAll(this.processFolder(childFolder));
            }
        }

        // and now the emails for this folder
        if (folder.getContentCount() > 0) {
            PSTMessage email = (PSTMessage) folder.getNextChild();
            while (email != null) {
                MimeMessage m = MessageCreator.createMessage(email.getReceivedByAddress(), email.getSenderEmailAddress(),email.getSubject(),email.getBody(),new ArrayList<>());
                messages.add(m);
                email = (PSTMessage) folder.getNextChild();
            }
        }
        return messages;
    }
}
