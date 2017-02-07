package uk.ac.cam.cl.charlie.mail;

import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.FLAGS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by Simon on 04/02/2017.
 */
public class LocalIMAPFolder {
    private static final Logger log = LoggerFactory.getLogger(LocalIMAPFolder.class);


    private ArrayList<LocalMessage> messages;
    private IMAPFolder serverFolder;
    private long highestUID;

    public LocalIMAPFolder(IMAPFolder folder) throws MessagingException, IOException {
        messages = new ArrayList<>();
        serverFolder = folder;
        initialCache();
    }

    private void checkFolderOpen() throws MessagingException {
        if (!serverFolder.isOpen()) {
            log.info("Attempted to operate on closed folder '{}', opened it.", serverFolder.getFullName());
            serverFolder.open(IMAPFolder.READ_WRITE);
        }
    }

    private void initialCache() throws MessagingException, IOException {
        checkFolderOpen();
        Message[] serverMessages = serverFolder.getMessages();

        log.info("{} messages were locally cached from folder '{}'.", serverMessages.length, serverFolder.getFullName());

        for (Message m : serverMessages) {
            messages.add(new LocalMessage(this, m));
        }
        updateHighestUID();
        serverFolder.close(false);
    }

    public void sync() throws MessagingException, IOException {
        checkFolderOpen();
        checkForMovedOrDeletedMessages();
        checkForNewMessages();

        serverFolder.close(true);
    }

    private void checkForMovedOrDeletedMessages() throws MessagingException, IOException {
        for (LocalMessage m : messages) m.setSynchedWithServer(false);

        for (Message m : serverFolder.getMessagesByUID(0L, highestUID)) {
            if (!this.checkSynchedWithServer(m)) {
                LocalMessage newMessage = new LocalMessage(this, m);
                newMessage.setSynchedWithServer(true);
                messages.add(newMessage);
            }
        }

        messages.removeIf(m -> !m.getSynchedWithServer());
    }

    private boolean checkSynchedWithServer(Message m) throws IOException, MessagingException {
        for (LocalMessage localMessage : messages) {
            if (localMessage.sameBackingMessageIgnoreUID(m)) {
                localMessage.setSynchedWithServer(true);
                return true;
            }
        }
        return false;
    }

    private void checkForNewMessages() throws MessagingException, IOException {
        Message[] newServerMessages = serverFolder.getMessagesByUID(highestUID + 1, UIDFolder.LASTUID);
        if (newServerMessages.length == 0) {
            log.info("No new messages were found in folder '{}'.", serverFolder.getFullName());
        } else {
            log.info("{} new messages were found in folder '{}'.", newServerMessages.length, serverFolder.getFullName());
            for (Message m : newServerMessages) {
                // For some reason, got back messages not in range specified
                if (serverFolder.getUID(m) > highestUID) {
                    messages.add(new LocalMessage(this, m));
                }
            }
            updateHighestUID();
        }
    }

    private void updateHighestUID() throws MessagingException {
        highestUID = serverFolder.getUID(messages.get(messages.size() - 1).getMessage());
    }

    public ArrayList<LocalMessage> getMessages() {
        return messages;
    }

    public long getUID(Message m) throws MessagingException {
        long uid;
        if (serverFolder.isOpen()) {
            uid = serverFolder.getUID(m);
        } else {
            checkFolderOpen();
            uid = serverFolder.getUID(m);
            serverFolder.close(false);
        }
        return uid;
    }

    public void moveMessages(LocalMessage[] localMessages, String subFolderName) throws MessagingException {
        checkFolderOpen();
        moveMessages(localMessages, serverFolder.getFolder(subFolderName));
    }

    public void moveMessages(LocalMessage[] localMessages, Folder f) throws MessagingException {
        Message[] imapMessages = Arrays.stream(localMessages).map(localMessage -> localMessage.getMessage()).toArray(size -> new Message[size]);
        moveMessages(imapMessages, f);
    }

    public void moveMessages(Message[] imapMessages, String subFolderName) throws MessagingException {
        checkFolderOpen();
        moveMessages(imapMessages, serverFolder.getFolder(subFolderName));
    }

    public void moveMessages(Message[] imapMessages, Folder f) throws MessagingException {
        checkFolderOpen();
        f.open(Folder.READ_WRITE);
        serverFolder.copyMessages(imapMessages, f);
        for (Message m : imapMessages) {
            m.setFlag(Flags.Flag.DELETED, true);
        }
        f.close(false);
        serverFolder.close(true);
    }

    public void listMessages() throws MessagingException, IOException {
        for (LocalMessage m : messages) {
            System.out.println(
                    "From: " + m.getFrom()[0].toString() + "\n"
                    + "To: " + m.getRecipients()[0].toString() + "\n"
                    + "Subject: " + m.getSubject() + "\n"
                    + "Content: " + m.getContent().toString() + "\n"
            );
        }
    }
}
