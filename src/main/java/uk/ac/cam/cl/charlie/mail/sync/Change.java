package uk.ac.cam.cl.charlie.mail.sync;

import javax.mail.MessagingException;

import org.slf4j.Logger;

import com.sun.mail.imap.IMAPFolder;

public abstract class Change {

    protected static Logger log = SyncIMAPStore.log;

    protected SyncIMAPFolder folder;

    public Change(SyncIMAPFolder folder) {
        this.folder = folder;
    }

    public abstract void perform(IMAPFolder folder) throws MessagingException;

}
