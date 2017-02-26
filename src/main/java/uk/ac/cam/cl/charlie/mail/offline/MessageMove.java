package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalMailRepresentation;
import uk.ac.cam.cl.charlie.mail.LocalMessage;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by Simon on 07/02/2017.
 */
public class MessageMove implements OfflineChange {

    private final LocalIMAPFolder sourceFolder;
    private final LocalIMAPFolder destinationFolder;
    private final LocalMessage[] messagesToMove;

    public MessageMove(LocalIMAPFolder sourceFolder, LocalIMAPFolder destinationFolder, LocalMessage... localMessages) {
        this.destinationFolder = destinationFolder;
        this.sourceFolder = sourceFolder;
        this.messagesToMove = localMessages;
    }

    @Override
    public void handleChange(LocalMailRepresentation mailRepresentation) throws MessagingException, IMAPConnectionClosedException, IOException {
        // copy messages to new folder
        sourceFolder.checkFolderOpen();
        long[] uids = new long[messagesToMove.length];
        for (int i = 0; i < uids.length; i++) {
            uids[i] = messagesToMove[i].getUID();
        }
        Message[] messages = sourceFolder.getBackingFolder().getMessagesByUID(uids);
        sourceFolder.getBackingFolder().copyMessages(messages, destinationFolder.getBackingFolder());

        // delete messages in old folder
        for (Message m : messages) {
            m.setFlag(Flags.Flag.DELETED, true);
        }
        sourceFolder.closeFolder(true);
    }
}
