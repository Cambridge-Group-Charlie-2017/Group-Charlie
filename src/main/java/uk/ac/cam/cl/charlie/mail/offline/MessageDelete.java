package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.IMAPConnection;
import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalMessage;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.MessagingException;
import java.io.IOException;

/**
 * Created by Simon on 07/02/2017.
 */
public class MessageDelete implements OfflineChange {

    private final LocalIMAPFolder parentFolder;
    private final LocalMessage message;

    public MessageDelete(LocalIMAPFolder folder, LocalMessage m) {
        parentFolder = folder;
        message = m;
    }

    @Override
    public void handleChange(IMAPConnection connection) throws IMAPConnectionClosedException, MessagingException, IOException {
        parentFolder.deleteMessages(message);
    }
}
