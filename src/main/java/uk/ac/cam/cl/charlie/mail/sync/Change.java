package uk.ac.cam.cl.charlie.mail.sync;

import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.imap.IMAPFolder;

public abstract class Change {

    protected static Logger log = LoggerFactory.getLogger(Change.class);

    protected SyncIMAPFolder folder;

    public Change(SyncIMAPFolder folder) {
        this.folder = folder;
    }

    public abstract void perform(IMAPFolder folder) throws MessagingException;

}
