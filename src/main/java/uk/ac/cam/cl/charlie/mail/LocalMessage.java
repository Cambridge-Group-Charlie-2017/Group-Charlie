package uk.ac.cam.cl.charlie.mail;

import com.sun.mail.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Date;

/**
 * Created by Simon on 05/02/2017.
 */
public class LocalMessage {
    private static final Logger log = LoggerFactory.getLogger(LocalMessage.class);


    private final LocalIMAPFolder localFolder;
    private final String subject;

    private final Address[] from;
    private final Address[] recipients;
    private final Date receivedDate;
    private final Object content;
    private final String contentType;

    private long UID;
    private int messageNumber;
    private Message message;
    private boolean synchedWithServer;
    private boolean isLocalOnly;

    public LocalMessage(LocalIMAPFolder folder, Message m) throws MessagingException, IOException, IMAPConnectionClosedException {
        localFolder = folder;
        message = m;
        subject = m.getSubject();
        from = m.getFrom();
        recipients = m.getAllRecipients();
        receivedDate = m.getReceivedDate();
        content = m.getContent();
        contentType = m.getContentType();
        UID = folder.getUID(message);
        messageNumber = m.getMessageNumber();

        synchedWithServer = false;
        isLocalOnly = false;
    }

    public boolean isLocalOnly() { return isLocalOnly; }

    public boolean hasConnection() {
        return message != null;
    }

    public void openConnection(Message m) throws IMAPConnectionClosedException, MessagingException {
        message = m;
        UID = localFolder.getUID(m);
        messageNumber = m.getMessageNumber();
        isLocalOnly = false;
    }

    public void closeConnection() {
        message = null;
    }

    public long getUID() { return UID; }
    public int getMessageNumber() { return messageNumber; }

    public boolean getSynchedWithServer() {
        return synchedWithServer;
    }

    public void setSynchedWithServer(boolean value) {
        synchedWithServer = value;
    }

    public Message getMessage() {
        return message;
    }

    public String getSubject() {
        return subject;
    }

    public Address[] getFrom() {
        return from;
    }

    public Address[] getRecipients() {
        return recipients;
    }

    public Date getReceivedDate() {
        return receivedDate;
    }

    public Object getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean sameBackingMessageIgnoreUID(Message m) throws MessagingException, IOException {
        return subject.equals(m.getSubject())
                && receivedDate.equals(m.getReceivedDate())
                && contentType.equals(m.getContentType())
                && content.toString().equals(m.getContent().toString());

    }

    public void setIsLocalOnly(boolean isLocalOnly) {
        this.isLocalOnly = isLocalOnly;
    }
}
