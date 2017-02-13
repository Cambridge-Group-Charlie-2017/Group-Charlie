package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalMailRepresentation;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderHoldsNoFoldersException;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.MessagingException;


/**
 * Created by Simon on 07/02/2017.
 */
public class FolderMove implements OfflineChange {

    private final LocalIMAPFolder folderToMove;
    private final LocalIMAPFolder newParentFolder;

    public FolderMove(LocalIMAPFolder folderToMove, LocalIMAPFolder newParentFolder) {
        this.folderToMove = folderToMove;
        this.newParentFolder = newParentFolder;
    }

    @Override
    public void handleChange(LocalMailRepresentation mailRepresentation) throws IMAPConnectionClosedException, MessagingException, FolderAlreadyExistsException, FolderHoldsNoFoldersException {
        folderToMove.moveFolder(newParentFolder);
    }
}
