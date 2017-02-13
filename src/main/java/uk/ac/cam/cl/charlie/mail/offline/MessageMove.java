package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.IMAPConnection;
import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalMessage;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.MessagingException;

/**
 * Created by Simon on 07/02/2017.
 */
public class MessageMove implements OfflineChange {

    private final LocalIMAPFolder sourceFolder;
    private final LocalIMAPFolder destinationFolder;
    private final LocalMessage messageToMove;

    public MessageMove(LocalIMAPFolder sourceFolder, LocalIMAPFolder destinationFolder, LocalMessage m) {
        this.destinationFolder = destinationFolder;
        this.sourceFolder = sourceFolder;
        this.messageToMove = m;
    }

    @Override
    public void handleChange(IMAPConnection connection) throws MessagingException, IMAPConnectionClosedException {
        sourceFolder.moveMessages(destinationFolder, messageToMove);
    }
}
