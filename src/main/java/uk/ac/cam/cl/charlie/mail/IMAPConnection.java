package uk.ac.cam.cl.charlie.mail;

import com.sun.mail.imap.IMAPFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderHoldsNoFoldersException;
import uk.ac.cam.cl.charlie.mail.exceptions.InvalidFolderNameException;

import java.util.*;
import javax.mail.*;

/**
 * Class for creating and manipulating an IMAP connection
 *
 * @author Simon Gielen
 */

public class IMAPConnection {

    private static final Logger log = LoggerFactory.getLogger(IMAPConnection.class);

    private final String host;
    private final String port;
    private final String provider;
    private final PasswordAuthentication authenticator;

    private Properties connectionProperties;
    private Session connectionSession;
    private Store sessionStore;

    public IMAPConnection(String host, String username, String password, String port, String provider) throws NoSuchProviderException {
        this.host = host;
        this.authenticator = new PasswordAuthentication(username, password);
        this.port = port;
        this.provider = provider;

        prepareConnection();
    }

    private void prepareConnection() throws NoSuchProviderException {
        connectionProperties = new Properties();
        connectionProperties.put("mail.store.protocol", provider);
        connectionProperties.put("mail.imap.host", host);
        connectionProperties.put("mail.imap.port", port);

        // Needed for pesky Certificate needs, might need to disable later for safety of the user?
        connectionProperties.put("mail.imaps.ssl.trust", "*");

        connectionSession = Session.getDefaultInstance(connectionProperties, null);
        sessionStore = connectionSession.getStore(provider);
    }

    public void connect() throws MessagingException {
        try {
            sessionStore.connect(host, authenticator.getUserName(), authenticator.getPassword());
            log.info("Connected to '{}' under username '{}'", host, authenticator.getUserName());
        } catch (AuthenticationFailedException e) {
            log.error("Failed to connect to '{}' under username '{}': Invalid credentials", host, authenticator.getUserName());
            throw e;
        }
    }

    public void close() throws MessagingException {
        if (!sessionStore.isConnected()) {
            log.info("Tried to close an already closed IMAP store");
            throw new StoreClosedException(sessionStore);
        }
        try {
            sessionStore.close();
            log.info("Closed connection to '{}' under username '{}'", host, authenticator.getUserName());
        } catch (MessagingException e) {
            log.error("Failed to close the connection to '{}' under username '{}'", host, authenticator.getUserName());
            throw e;
        }
    }

    public IMAPFolder[] getAllFolders() throws MessagingException {
        return getAllFolders(sessionStore.getDefaultFolder());
    }
    public IMAPFolder[] getAllFolders(Folder rootFolder) throws MessagingException {
        if (!sessionStore.isConnected()) {
            log.error("Tried to operate on a closed store.");
            throw new IllegalStateException("Store is closed, connection is not established.");
        }
        return (IMAPFolder[]) rootFolder.list("*");
    }

    public IMAPFolder getDefaultFolder() throws MessagingException {
        return (IMAPFolder) sessionStore.getDefaultFolder();
    }
    public IMAPFolder getFolder(String name) throws MessagingException {
        return (IMAPFolder) sessionStore.getFolder(name);
    }

    public void createFolder(String newFolderName) throws MessagingException, FolderAlreadyExistsException, FolderHoldsNoFoldersException, InvalidFolderNameException {
        createFolder((IMAPFolder) sessionStore.getDefaultFolder(), newFolderName);
    }

    public void createFolder(IMAPFolder parentFolder, String newFolderName) throws FolderAlreadyExistsException, MessagingException, FolderHoldsNoFoldersException, InvalidFolderNameException {
        if (newFolderName.contains(".")) {
            log.error("Invalid folder name '{}' with parent '{}'", newFolderName, parentFolder.getFullName());
            throw new InvalidFolderNameException("The folder name can't contain a '.'");
        }

        if (!parentFolder.exists()) {
            log.error("Attemted to create subfolder '{}' under non exisiting parent folder '{}'", newFolderName, parentFolder.getFullName());
            throw new FolderNotFoundException();
        }
        if ((parentFolder.getType() & Folder.HOLDS_FOLDERS) == 0) {
            log.error("Attemted to create subfolder in '{}' under parent folder '{}', which does not hold folders", newFolderName, parentFolder.getFullName());
            throw new FolderHoldsNoFoldersException();
        }

        try {
            Folder newFolder = parentFolder.getFolder(newFolderName);
            if (newFolder.exists()) throw new FolderAlreadyExistsException();
            newFolder.create(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS);
        } catch (MessagingException e) {
            log.error("Error creating folder {} with parent {}: {}", newFolderName, parentFolder.getFullName(), e.getMessage());
            throw e;
        }
    }

    public List<Message> getAllMessagesInFolderTree(IMAPFolder rootFolder) throws MessagingException {
        List<Message> messages = new ArrayList<>();
        for (Folder f : getAllFolders(rootFolder)) {
            Collections.addAll(messages, f.getMessages());
        }
        return messages;
    }
}
