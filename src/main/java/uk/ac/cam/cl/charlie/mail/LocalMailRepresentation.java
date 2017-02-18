package uk.ac.cam.cl.charlie.mail;

import com.sun.mail.imap.IMAPFolder;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderHoldsNoFoldersException;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;
import uk.ac.cam.cl.charlie.mail.exceptions.InvalidFolderNameException;
import uk.ac.cam.cl.charlie.mail.offline.FolderCreation;
import uk.ac.cam.cl.charlie.mail.offline.OfflineChangeList;

import javax.mail.MessagingException;
import java.io.IOException;

/**
 * Created by Simon on 13/02/2017.
 */
public class LocalMailRepresentation {

    private LocalIMAPFolder rootFolder;
    private IMAPConnection imapConnection;

    public LocalMailRepresentation(IMAPConnection imapConnection) throws MessagingException, IOException, IMAPConnectionClosedException {
        rootFolder = new LocalIMAPFolder(null, imapConnection.getDefaultFolder());
        imapConnection.setLocalMailRepresentation(this);
        this.imapConnection = imapConnection;
    }

    private boolean hasConnection() throws IMAPConnectionClosedException {
        return imapConnection != null;
    }

    public void syncAllFolders() throws IMAPConnectionClosedException, MessagingException, IOException {
        if (!hasConnection()) throw new IMAPConnectionClosedException();
        syncRecursive(rootFolder);
    }

    private void syncRecursive(LocalIMAPFolder folder) throws IMAPConnectionClosedException, MessagingException, IOException {
        folder.sync();
        for (LocalIMAPFolder f : folder.getSubfolders().values()) {
            syncRecursive(f);
        }
    }

    public LocalIMAPFolder getRootFolder() {
        return rootFolder;
    }

    public LocalIMAPFolder getFolder(String name) throws MessagingException, IOException, IMAPConnectionClosedException {
        LocalIMAPFolder folder = rootFolder.getFolder(name.toLowerCase());
        if (folder.hasConnection()) folder.sync();
        return folder;
    }

    public LocalIMAPFolder createFolder(String newFolderName) throws FolderHoldsNoFoldersException, InvalidFolderNameException, IMAPConnectionClosedException, IOException, FolderAlreadyExistsException, MessagingException {
        return createFolder(rootFolder, newFolderName);
    }

    public LocalIMAPFolder createFolder(LocalIMAPFolder parentFolder, String newFolderName) throws MessagingException, FolderHoldsNoFoldersException, FolderAlreadyExistsException, InvalidFolderNameException, IOException, IMAPConnectionClosedException {
        LocalIMAPFolder localFolder;
        if (!hasConnection()) {
            localFolder = new LocalIMAPFolder(parentFolder, newFolderName);
            OfflineChangeList.getInstance().addChange(new FolderCreation(parentFolder, newFolderName));
        } else {
            IMAPFolder createdFolder = imapConnection.createFolder(parentFolder.getBackingFolder(), newFolderName);
            localFolder = new LocalIMAPFolder(parentFolder, createdFolder);
        }
        parentFolder.addSubfolder(localFolder);
        return localFolder;
    }


    public void connectionClosed() throws MessagingException {
        rootFolder.closeConnection();
        imapConnection = null;
    }

    public void setConnection(IMAPConnection imapConnection) throws IMAPConnectionClosedException, MessagingException, IOException, FolderHoldsNoFoldersException, FolderAlreadyExistsException, InvalidFolderNameException {
        this.imapConnection = imapConnection;
        openConnectionRecursive(rootFolder);
        syncRecursive(rootFolder);
        OfflineChangeList.getInstance().performChanges(this);
    }

    private void openConnectionRecursive(LocalIMAPFolder rootFolder) throws IMAPConnectionClosedException, MessagingException, IOException {
        rootFolder.openConnection(imapConnection);
        for (LocalIMAPFolder f : rootFolder.getSubfolders().values()) {
            openConnectionRecursive(f);
        }
    }

    public void deleteFolder(LocalIMAPFolder folderToDelete) throws MessagingException {
        folderToDelete.getParentFolder().removeSubfolder(folderToDelete);
        folderToDelete.delete();
    }
}
