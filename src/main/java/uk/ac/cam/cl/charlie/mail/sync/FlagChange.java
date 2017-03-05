package uk.ac.cam.cl.charlie.mail.sync;

import javax.mail.Flags;
import javax.mail.MessagingException;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;

public class FlagChange extends Change {

    long uid;
    Flags flags;
    boolean set;

    public FlagChange(SyncIMAPFolder folder, long uid, Flags flags, boolean set) {
        super(folder);

        this.uid = uid;
        this.flags = flags;
        this.set = set;
    }

    @Override
    public void perform(IMAPFolder imapFolder) throws MessagingException {
        log.info("Performing flag change on {}/{}", folder.getFullName(), uid);
        IMAPMessage msg = (IMAPMessage) imapFolder.getMessageByUID(uid);
        msg.setFlags(flags, set);
    }

}
