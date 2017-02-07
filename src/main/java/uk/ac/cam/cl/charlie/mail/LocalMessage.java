package uk.ac.cam.cl.charlie.mail;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.util.Date;

/**
 * Created by Simon on 05/02/2017.
 */
public class LocalMessage {
    private final LocalIMAPFolder localFolder;
    private final Message message;

    private final String subject;
    private final Address[] from;
    private final Address[] recipients;
    private final Date receivedDate;
    private final Object content;
    private final String contentType;

    private boolean synchedWithServer;

    public LocalMessage(LocalIMAPFolder folder, Message m) throws MessagingException, IOException {
        localFolder = folder;
        message = m;
        subject = m.getSubject();
        from = m.getFrom();
        recipients = m.getAllRecipients();
        receivedDate = m.getReceivedDate();
        content = m.getContent();
        contentType = m.getContentType();

        synchedWithServer = false;
    }

    public boolean getSynchedWithServer() {
        return synchedWithServer;
    }

    public void setSynchedWithServer(boolean value) {
        synchedWithServer = value;
    }

    public Message getMessage() {
        return message;
    }

    public long getUID() throws MessagingException {
        return localFolder.getUID(message);
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
}
