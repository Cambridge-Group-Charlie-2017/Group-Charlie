package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalMailRepresentation;
import uk.ac.cam.cl.charlie.mail.LocalMessage;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;

/**
 * Created by Simon on 07/02/2017.
 */
public class MessageDelete implements OfflineChange {

    private final LocalIMAPFolder parentFolder;
    private final LocalMessage[] messagesToDelete;

    public MessageDelete(LocalIMAPFolder folder, LocalMessage... localMessages) {
        parentFolder = folder;
        messagesToDelete = localMessages;
    }

    @Override
    public void handleChange(LocalMailRepresentation mailRepresentation) throws IMAPConnectionClosedException, MessagingException, IOException {
        parentFolder.checkFolderOpen();
        long[] uids = new long[messagesToDelete.length];
        for (int i = 0; i < uids.length; i++) {
            uids[i] = messagesToDelete[i].getUID();
        }
        for (Message m : parentFolder.getBackingFolder().getMessagesByUID(uids)) {
            m.setFlag(Flags.Flag.DELETED, true);
        }
        parentFolder.getBackingFolder().close(true);
    }
}
