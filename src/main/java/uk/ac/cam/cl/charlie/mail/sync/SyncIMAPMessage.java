package uk.ac.cam.cl.charlie.mail.sync;

import java.io.IOException;
import java.io.InputStream;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

public class SyncIMAPMessage extends MimeMessage {

    long uid;
    boolean initialized = false;

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
            initialized = false;

            // If content is not yet loaded, then force the folder to download
            // the message
            SyncIMAPFolder folder = (SyncIMAPFolder) this.folder;
            folder.downloadMessage(uid);
            byte[] bytes = folder.map.get(uid);

            if (folder.deserializeStatus(bytes) == 2) {
                folder.deserializeWithContent(bytes, this);
            } else {
                throw new MessagingException("Cannot download message");
            }

            initialized = true;
        }
    }

    @Override
    public Object getContent() throws MessagingException, IOException {
        checkContent();
        return super.getContent();
    }

    public Object fastGetContent() throws MessagingException, IOException {
        if (content == null) {
            return null;
        }
        return super.getContent();
    }

    @Override
    public void setFlags(Flags flag, boolean set) throws MessagingException {
        super.setFlags(flag, set);

        if (!initialized)
            return;

        // Queue changes subject to synchronization
        SyncIMAPFolder folder = (SyncIMAPFolder) this.folder;
        folder.enqueueChange(new FlagChange(folder, uid, flag, set));

        folder.flushChange(this);
        folder.synchronize();
    }

    protected void overrideFlags(Flags flag) throws MessagingException {
        this.flags = flag;

        SyncIMAPFolder folder = (SyncIMAPFolder) this.folder;
        folder.flushChange(this);
    }

}
