package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalMessage;

/**
 * Created by Simon on 07/02/2017.
 */
public class MessageMove implements OfflineChange {

    private final LocalIMAPFolder parentFolder;
    private final LocalMessage messageToMove;

    public MessageMove(LocalIMAPFolder folder, LocalMessage m) {
        parentFolder = folder;
        messageToMove = m;
    }

    @Override
    public void handleChange() {

    }
}
