package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.IMAPConnection;
import uk.ac.cam.cl.charlie.mail.LocalMailRepresentation;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderHoldsNoFoldersException;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;
import uk.ac.cam.cl.charlie.mail.exceptions.InvalidFolderNameException;

import javax.mail.MessagingException;
import java.io.IOException;

/**
 * Created by Simon on 07/02/2017.
 */
public interface OfflineChange {
    void handleChange(LocalMailRepresentation mailRepresentation) throws FolderHoldsNoFoldersException, InvalidFolderNameException, IMAPConnectionClosedException, IOException, FolderAlreadyExistsException, MessagingException;
}
