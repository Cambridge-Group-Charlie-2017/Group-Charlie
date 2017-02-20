package uk.ac.cam.cl.charlie.mail.offline;

import com.sun.mail.imap.IMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalMailRepresentation;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.Folder;
import javax.mail.MessagingException;
import java.io.IOException;


/**
 * Created by Simon on 07/02/2017.
 */
public class FolderMove implements OfflineChange {

    private final String originalFolderPath;
    private final String newFolderPath;
    private final char hierarchicalSeparator;

    public FolderMove(String originalFolderPath, String newParentFolderPath, char hierarchicalSeparator) {
        this.hierarchicalSeparator = hierarchicalSeparator;
        this.originalFolderPath = originalFolderPath;
        String originalFolderName = originalFolderPath.substring(originalFolderPath.lastIndexOf(hierarchicalSeparator) + 1);
        this.newFolderPath = String.join(Character.toString(hierarchicalSeparator), newParentFolderPath, originalFolderName);
    }

    @Override
    public void handleChange(LocalMailRepresentation mailRepresentation) throws IMAPConnectionClosedException, MessagingException, IOException, FolderAlreadyExistsException {
        IMAPFolder rootFolder = mailRepresentation.getRootFolder().getBackingFolder();
        IMAPFolder originalFolder = (IMAPFolder) rootFolder.getFolder(originalFolderPath);
        recursiveMove(rootFolder, originalFolder, newFolderPath, hierarchicalSeparator);
    }

    private static void recursiveMove(IMAPFolder rootFolder, IMAPFolder folderToMove, String newFolderPath, char separator) throws MessagingException, FolderAlreadyExistsException {
        // Get a reference to the new folder location
        Folder newFolder = rootFolder.getFolder(newFolderPath);

        // Make sure it doesn't exist, fail if it does
        if (newFolder.exists()) {
            throw new FolderAlreadyExistsException();
        }

        // Rename the folder
        int type = folderToMove.getType();
        Folder[] subFolders = folderToMove.list(LocalIMAPFolder.getSubfolderListMatcher(folderToMove.getFullName(), separator));
        folderToMove.renameTo(newFolder);

        // Recursive call for all the sub folders
        if ((type & Folder.HOLDS_FOLDERS) != 0) {
            for (Folder f : subFolders) {
                String newPath = String.join(Character.toString(separator), folderToMove.getFullName(), f.getName());
                recursiveMove(rootFolder, (IMAPFolder) f, newPath, separator);
            }
        }
    }
}
