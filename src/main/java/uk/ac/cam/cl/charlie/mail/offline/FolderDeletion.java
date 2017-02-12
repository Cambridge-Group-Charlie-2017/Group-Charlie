package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.IMAPConnection;
import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;

/**
 * Created by Simon on 07/02/2017.
 */
public class FolderDeletion implements OfflineChange {
    private final LocalIMAPFolder folderToDelete;

    public FolderDeletion(LocalIMAPFolder folder) {
        folderToDelete = folder;
    }

    @Override
    public void handleChange(IMAPConnection connection) {

    }
}
