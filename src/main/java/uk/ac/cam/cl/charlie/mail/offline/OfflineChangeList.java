package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.IMAPConnection;
import uk.ac.cam.cl.charlie.mail.LocalMailRepresentation;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderHoldsNoFoldersException;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;
import uk.ac.cam.cl.charlie.mail.exceptions.InvalidFolderNameException;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Stack;

/**
 * Created by Simon on 07/02/2017.
 */
public class OfflineChangeList {
    Stack<OfflineChange> changes;

    public OfflineChangeList() {
        changes = new Stack<>();
    }

    public void performChanges(LocalMailRepresentation mailRepresentation) throws MessagingException, IMAPConnectionClosedException, FolderAlreadyExistsException, FolderHoldsNoFoldersException, IOException, InvalidFolderNameException {
        while (!changes.isEmpty()) {
            changes.pop().handleChange(mailRepresentation);
        }
    }

    public void addChange(OfflineChange change) {
        changes.push(change);
    }

    public void clearChanges() {
        changes.clear();
    }
}
