package uk.ac.cam.cl.charlie.mail;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class SyncIMAPMessage extends MimeMessage {

    long uid;

    public SyncIMAPMessage(SyncIMAPFolder folder, long uid) throws MessagingException {
        super((Session) null);

        this.folder = folder;
        this.uid = uid;
    }

    @Override
    protected void parse(InputStream is) throws MessagingException {
        // Override this to expose the function to SyncIMAPFolder
        super.parse(is);
    }

    private void checkContent() throws MessagingException {
        if (content == null) {
            // If content is not yet loaded, then force the folder to download
            // the message
            SyncIMAPFolder folder = (SyncIMAPFolder) this.folder;
            folder.synchronizeMessage(uid);
            folder.deserializeWithContent(folder.map.get(uid), this);
        }
    }

    @Override
    public Object getContent() throws MessagingException, IOException {
        checkContent();
        return super.getContent();
    }

}
