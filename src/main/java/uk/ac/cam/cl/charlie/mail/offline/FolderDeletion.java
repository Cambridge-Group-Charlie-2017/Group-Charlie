package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalMailRepresentation;

import javax.mail.MessagingException;

/**
 * Created by Simon on 07/02/2017.
 */
public class FolderDeletion implements OfflineChange {
    private final LocalIMAPFolder folderToDelete;

    public FolderDeletion(LocalIMAPFolder folder) {
        folderToDelete = folder;
    }

    @Override
    public void handleChange(LocalMailRepresentation mailRepresentation) throws MessagingException {
        mailRepresentation.getImapConnection().getFolder(folderToDelete.getFullName()).delete(true);
    }
}
