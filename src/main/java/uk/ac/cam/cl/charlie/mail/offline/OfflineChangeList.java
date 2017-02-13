package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.IMAPConnection;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderHoldsNoFoldersException;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.MessagingException;
import java.util.Stack;

/**
 * Created by Simon on 07/02/2017.
 */
public class OfflineChangeList {
    private final static OfflineChangeList instance = new OfflineChangeList();
    public static OfflineChangeList getInstance() {
        return instance;
    }


    Stack<OfflineChange> changes;

    private OfflineChangeList() {
        changes = new Stack<>();
    }

    public void performChanges(IMAPConnection connection) throws MessagingException, IMAPConnectionClosedException, FolderAlreadyExistsException, FolderHoldsNoFoldersException {
        while (!changes.isEmpty()) {
            changes.pop().handleChange(connection);
        }
    }

    public void addChange(OfflineChange change) {
        changes.push(change);
    }

    public void clearChanges() {
        changes.clear();
    }
}
