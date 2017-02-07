package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalMessage;

/**
 * Created by Simon on 07/02/2017.
 */
public class MessageCreate implements OfflineChange {

    private final LocalIMAPFolder parentFolder;
    public final LocalMessage message;

    public MessageCreate(LocalIMAPFolder folder, LocalMessage m) {
        parentFolder = folder;
        message = m;
    }

    @Override
    public void handleChange() {

    }
}
