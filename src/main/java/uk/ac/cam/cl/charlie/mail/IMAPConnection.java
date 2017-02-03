package uk.ac.cam.cl.charlie.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        sessionStore     = connectionSession.getStore(provider);
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
        try {
            sessionStore.close();
            log.info("Closed connection to '{}' under username '{}'", host, authenticator.getUserName());
        } catch (MessagingException e) {
            log.error("Failed to close the connection to '{}' under username '{}'", host, authenticator.getUserName());
            throw e;
        }
    }

    public Folder[] getAllFolders() throws IMAPConnectionClosedException, MessagingException {
        if (!sessionStore.isConnected()) throw new IMAPConnectionClosedException();
        return sessionStore.getDefaultFolder().list("*");
    }

    public Folder getFolder(String name) throws MessagingException {
        return sessionStore.getFolder(name);
    }

    public void createFolder(String newFolderName) throws MessagingException, FolderAlreadyExistsException {
        createFolder(sessionStore.getDefaultFolder(), newFolderName);
    }

    public void createFolder(Folder parentFolder, String newFolderName) throws FolderAlreadyExistsException, MessagingException {
        if (!parentFolder.exists()) throw new FolderNotFoundException();
        try {
            Folder newFolder = parentFolder.getFolder(newFolderName);
            if (newFolder.exists()) throw new FolderAlreadyExistsException();
            newFolder.create(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS);
        } catch (MessagingException e) {
            log.error("Error creating folder {} with parent {}", newFolderName, parentFolder.getFullName());
            throw e;
        }
    }
}
