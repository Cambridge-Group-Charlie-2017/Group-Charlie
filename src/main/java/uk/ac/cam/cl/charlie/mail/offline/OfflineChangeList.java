package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.LocalMailRepresentation;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderHoldsNoFoldersException;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;
import uk.ac.cam.cl.charlie.mail.exceptions.InvalidFolderNameException;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by Simon on 07/02/2017.
 */
public class OfflineChangeList {
    private static OfflineChangeList instance = new OfflineChangeList();
    public static OfflineChangeList getInstance() { return instance; }


    Queue<OfflineChange> changes;

    private OfflineChangeList() {
        changes = new ArrayDeque<>();
    }

    public void performChanges(LocalMailRepresentation mailRepresentation) throws MessagingException, IMAPConnectionClosedException, FolderAlreadyExistsException, FolderHoldsNoFoldersException, IOException, InvalidFolderNameException {
        while (!changes.isEmpty()) {
            changes.poll().handleChange(mailRepresentation);
        }
    }

    public void addChange(OfflineChange change) {
        changes.add(change);
    }

    public void clearChanges() {
        changes.clear();
    }
}
