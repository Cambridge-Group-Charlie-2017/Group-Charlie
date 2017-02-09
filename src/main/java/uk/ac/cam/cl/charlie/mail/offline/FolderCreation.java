package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.IMAPConnection;
import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;

/**
 * Created by Simon on 07/02/2017.
 */
public class FolderCreation implements OfflineChange {
    private final LocalIMAPFolder parentFolder;
    private final String folderName;

    public FolderCreation(LocalIMAPFolder parentFolder, String folderName) {
        this.parentFolder = parentFolder;
        this.folderName = folderName;
    }

    @Override
    public void handleChange(IMAPConnection connection) {

    }
}
