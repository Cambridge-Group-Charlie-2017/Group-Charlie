package uk.ac.cam.cl.charlie.mail.offline;

import uk.ac.cam.cl.charlie.mail.IMAPConnection;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.MessagingException;

/**
 * Created by Simon on 07/02/2017.
 */
public interface OfflineChange {
    void handleChange(IMAPConnection connection) throws MessagingException, IMAPConnectionClosedException;
}
