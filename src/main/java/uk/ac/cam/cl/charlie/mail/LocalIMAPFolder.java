package uk.ac.cam.cl.charlie.mail;

import com.sun.mail.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderHoldsNoFoldersException;
import uk.ac.cam.cl.charlie.mail.exceptions.IMAPConnectionClosedException;

import javax.mail.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Created by Simon on 04/02/2017.
 */
public class LocalIMAPFolder {
    private static final Logger log = LoggerFactory.getLogger(LocalIMAPFolder.class);

    private ArrayList<LocalMessage> messages;
    private IMAPFolder serverFolder;
    private LocalIMAPFolder parentFolder;
    private HashMap<String, LocalIMAPFolder> subfolders;
    private String fullName;
    private long highestUID;
    private long UIDValidityValue;

    public LocalIMAPFolder(IMAPFolder folder) throws MessagingException, IOException, IMAPConnectionClosedException {
        serverFolder = folder;
        messages = new ArrayList<>();
        fullName = folder.getFullName();
        UIDValidityValue = folder.getUIDValidity();

        int depth = fullName.lastIndexOf(".");
        if (depth == -1) parentFolder = null;
        else parentFolder = getRootFolder().getFolder(fullName.substring(0, depth));

        subfolders = new HashMap<>();
        if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) createSubfolders();
        if ((folder.getType() & Folder.HOLDS_MESSAGES) != 0) initialCache();
    }

    private void createSubfolders() throws MessagingException, IOException, IMAPConnectionClosedException {
        for (Folder f : serverFolder.list("*")) {
            subfolders.put(f.getName().toLowerCase(), new LocalIMAPFolder((IMAPFolder) f));
        }
    }

    public HashMap<String, LocalIMAPFolder> getSubfolders() { return subfolders; }

    public boolean hasConnection() {
        return serverFolder != null;
    }

    public void openConnection(IMAPConnection connection) throws MessagingException, IOException, IMAPConnectionClosedException {
        log.info("Connected to server folder of '{}'.", fullName);
        serverFolder = connection.getFolder(fullName);
        if ((serverFolder.getType() & Folder.HOLDS_MESSAGES) != 0) {
            checkFolderOpen();

            if (serverFolder.getUIDValidity() != UIDValidityValue) {
                log.error("UID invalid for folder '{}', local cache cleared.", fullName);
                messages.clear();
                initialCache();
                return;
            }
            log.info("Upon reconnecting folder '{}' UID was valid.", fullName);

            for (LocalMessage m : messages) {
                // TODO: what if it can't find a message bc it has moves?
                Message serverMessage = serverFolder.getMessageByUID(m.getUID());
                m.openConnection(serverMessage);
            }
            serverFolder.close(false);

        }
        for (LocalIMAPFolder f : subfolders.values()) {
            f.openConnection(connection);
        }
    }

    public void closeConnection() throws MessagingException {
        log.info("Connection of folder '{}' was closed.", fullName);
        if (serverFolder != null && serverFolder.isOpen()) serverFolder.close(true);
        for (LocalMessage m : messages) m.closeConnection();
        for (LocalIMAPFolder f : subfolders.values()) f.closeConnection();
        serverFolder = null;
    }

    public void checkFolderOpen() throws MessagingException, IMAPConnectionClosedException {
        if (!hasConnection()) throw new IMAPConnectionClosedException();
        if (!serverFolder.isOpen()) {
            log.info("Attempted to operate on closed folder '{}', opened it.", serverFolder.getFullName());
            serverFolder.open(IMAPFolder.READ_WRITE);
        }
    }

    public void closeFolder(boolean expunge) throws MessagingException {
        serverFolder.close(expunge);
    }

    private void initialCache() throws MessagingException, IOException, IMAPConnectionClosedException {
        checkFolderOpen();
        Message[] serverMessages = serverFolder.getMessages();

        log.info("{} messages were locally cached from folder '{}'.", serverMessages.length, serverFolder.getFullName());

        for (Message m : serverMessages) {
            messages.add(new LocalMessage(this, m));
        }
        updateHighestUID();
        serverFolder.close(false);
    }

    public LocalIMAPFolder getRootFolder() {
        if (parentFolder == null) return this;
        else return parentFolder.getRootFolder();
    }

    public IMAPFolder getBackingFolder() {
        return serverFolder;
    }

    public LocalIMAPFolder getFolder(String name) throws MessagingException {
        LocalIMAPFolder f;
        if (name.contains(".")) {
            int firstIndex = name.indexOf(".");
            f = subfolders.get(name.substring(0, firstIndex)).getFolder(name.substring(firstIndex + 1));
        } else {
            f = subfolders.get(name);
        }
        if (f == null) throw new FolderNotFoundException();
        return f;
    }

    public String getFullName() {
        return fullName;
    }

    public String getName() { return fullName.substring(fullName.lastIndexOf(".") + 1); }

    public void sync() throws MessagingException, IOException, IMAPConnectionClosedException {
        if ((serverFolder.getType() & Folder.HOLDS_MESSAGES) != 0) {
            checkFolderOpen();
            checkForMovedOrDeletedMessages();
            checkForNewMessages();
        }
        checkNewFolders();

        if (serverFolder.isOpen()) serverFolder.close(true);
    }

    private void checkNewFolders() throws MessagingException, IOException, IMAPConnectionClosedException {
        for (Folder f : serverFolder.list("*")) {
            subfolders.putIfAbsent(f.getName().toLowerCase(), new LocalIMAPFolder((IMAPFolder) f));
        }
    }

    private void checkForMovedOrDeletedMessages() throws MessagingException, IOException, IMAPConnectionClosedException {
        for (LocalMessage m : messages) m.setSynchedWithServer(false);
        // throws javax.mail.FolderClosedException: * BYE JavaMail Exception: java.io.IOException: Connection dropped by server?
        // if no messages are present???
        if (serverFolder.getMessageCount() != 0) {
            for (Message m : serverFolder.getMessagesByUID(0L, highestUID)) {
                if (!this.checkSynchedWithServer(m)) {
                    LocalMessage newMessage = new LocalMessage(this, m);
                    newMessage.setSynchedWithServer(true);
                    messages.add(newMessage);
                }
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

    private void checkForNewMessages() throws MessagingException, IOException, IMAPConnectionClosedException {
        Message[] newServerMessages;
        if (serverFolder.getMessageCount() != 0) {
            newServerMessages = serverFolder.getMessagesByUID(highestUID + 1, UIDFolder.LASTUID);
        } else {
            newServerMessages = new Message[0];
        }
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
        if (messages.size() == 0) highestUID = 0L;
        else highestUID = serverFolder.getUID(messages.get(messages.size() - 1).getMessage());
    }

    public ArrayList<LocalMessage> getMessages() {
        return messages;
    }

    public long getUID(Message m) throws MessagingException, IMAPConnectionClosedException {
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

    public void moveMessages(String subFolderName, LocalMessage... localMessages) throws MessagingException, IMAPConnectionClosedException {
        checkFolderOpen();
        moveMessages(this.getFolder(subFolderName), localMessages);
    }

    public void moveMessages(LocalIMAPFolder f, LocalMessage... localMessages) throws MessagingException, IMAPConnectionClosedException {
        Message[] imapMessages = Arrays.stream(localMessages).map(LocalMessage::getMessage).toArray(Message[]::new);
        moveMessages(f, imapMessages);
    }

    public void moveMessages(String subFolderName, Message... imapMessages) throws MessagingException, IMAPConnectionClosedException {
        checkFolderOpen();
        moveMessages(this.getFolder(subFolderName), imapMessages);
    }

    public void moveMessages(LocalIMAPFolder f, Message... imapMessages) throws MessagingException, IMAPConnectionClosedException {
        checkFolderOpen();
        f.checkFolderOpen();
        // TODO: Check presence of all messages in current folder
        serverFolder.copyMessages(imapMessages, f.getBackingFolder());
        for (Message m : imapMessages) {
            m.setFlag(Flags.Flag.DELETED, true);
        }
        f.closeFolder(false);
        serverFolder.close(true);
    }

    public void deleteMessages(LocalMessage... localMessages) throws MessagingException, IMAPConnectionClosedException {
        Message[] imapMessages = Arrays.stream(localMessages).map(LocalMessage::getMessage).toArray(Message[]::new);
        deleteMessages(imapMessages);
    }

    public void deleteMessages(Message... imapMessages) throws MessagingException, IMAPConnectionClosedException {
        checkFolderOpen();
        for (Message m : imapMessages) {
            m.setFlag(Flags.Flag.DELETED, true);
        }
        serverFolder.close(true);
    }

    public void moveFolder(LocalIMAPFolder newParentFolder) throws MessagingException, IMAPConnectionClosedException, FolderAlreadyExistsException, FolderHoldsNoFoldersException {
        IMAPFolder oldServerFolder = serverFolder;

        moveFolderRecursive(newParentFolder);

        oldServerFolder.close(false);
        oldServerFolder.delete(true);
        parentFolder.removeSubfolder(this);
        newParentFolder.addSubfolder(this);
    }

    private void moveFolderRecursive(LocalIMAPFolder newParentFolder) throws MessagingException, IMAPConnectionClosedException, FolderAlreadyExistsException, FolderHoldsNoFoldersException {
        if ((newParentFolder.getBackingFolder().getType() & Folder.HOLDS_FOLDERS) == 0) {
            throw new FolderHoldsNoFoldersException();
        }

        this.checkFolderOpen();
        newParentFolder.checkFolderOpen();

        // Create new folder
        IMAPFolder movedFolder = (IMAPFolder) newParentFolder.getBackingFolder().getFolder(serverFolder.getName());
        if (movedFolder.exists()) {
            throw new FolderAlreadyExistsException();
        }
        movedFolder.create(serverFolder.getType());
        movedFolder.open(Folder.READ_WRITE);

        // Copy messages
        serverFolder.copyMessages(serverFolder.getMessages(), movedFolder);

        // Update current folder
        serverFolder = movedFolder;
        fullName = serverFolder.getFullName();
        UIDValidityValue = serverFolder.getUIDValidity();
        parentFolder = newParentFolder;


        // Recursive call
        if ((serverFolder.getType() & Folder.HOLDS_FOLDERS) != 0) {
            for (LocalIMAPFolder f : subfolders.values()) {
                f.moveFolderRecursive(this);
            }
        }


    }

    protected void addSubfolder(LocalIMAPFolder folder) {
        subfolders.put(folder.getName().toLowerCase(), folder);
    }

    protected void removeSubfolder(LocalIMAPFolder folder) {
        subfolders.remove(folder.getName().toLowerCase());
    }
}
