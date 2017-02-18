package uk.ac.cam.cl.charlie.mail.offline;

import com.sun.mail.imap.IMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalMailRepresentation;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderHoldsNoFoldersException;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.Folder;
import javax.mail.MessagingException;


/**
 * Created by Simon on 07/02/2017.
 */
public class FolderMove implements OfflineChange {

    private final String originalFolderPath;
    private final String newParentFolderPath;
    private final String newFolderPath;

    public FolderMove(String originalFolderPath, String newParentFolderPath) {
        this.originalFolderPath = originalFolderPath;
        this.newParentFolderPath = newParentFolderPath;
        this.newFolderPath = String.join(".", newParentFolderPath, originalFolderPath.substring(originalFolderPath.lastIndexOf(".") + 1));
    }

    @Override
    public void handleChange(LocalMailRepresentation mailRepresentation) throws IMAPConnectionClosedException, MessagingException, FolderAlreadyExistsException, FolderHoldsNoFoldersException {
        Folder rootFolder = mailRepresentation.getRootFolder().getBackingFolder();
        IMAPFolder folderToMove = (IMAPFolder) rootFolder.getFolder(originalFolderPath);
        IMAPFolder newParentFolder = (IMAPFolder) rootFolder.getFolder(newParentFolderPath);

        IMAPFolder movedFolder = (IMAPFolder) newParentFolder.getFolder(newFolderPath);
        if (movedFolder.exists()) {
            throw new FolderAlreadyExistsException();
        }
        movedFolder.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
        movedFolder.open(Folder.READ_WRITE);
        folderToMove.copyMessages(folderToMove.getMessages(), movedFolder);
    }
}
