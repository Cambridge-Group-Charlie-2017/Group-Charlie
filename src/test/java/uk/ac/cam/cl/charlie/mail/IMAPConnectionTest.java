package uk.ac.cam.cl.charlie.mail;


import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.*;

import static org.junit.Assert.assertEquals;

/**
 * Test for class {@link IMAPConnection}
 *
 * @author Simon Gielen
 */

public class IMAPConnectionTest {
    private GreenMail mailServer;
    private static final String USER_NAME = "GROUP-CHARLIE";
    private static final String USER_EMAIL_ADDRESS  = "GROUP-CHARLIE@cam.ac.uk";
    private static final String USER_PASSWORD = "abcdef123";
    private static final String LOCALHOST = "127.0.0.1";

    private static IMAPConnection imapConnection;


    @Before
    public void setUp() throws NoSuchProviderException {
        mailServer = new GreenMail(ServerSetup.IMAPS);
        mailServer.start();

        mailServer.setUser(USER_EMAIL_ADDRESS, USER_NAME, USER_PASSWORD);

        imapConnection = new IMAPConnection(
                LOCALHOST,
                USER_NAME,
                USER_PASSWORD,
                Integer.toString(ServerSetup.IMAPS.getPort()),
                "imaps"
        );
    }

    @After
    public void tearDown() {
        mailServer.stop();
    }

    @Test
    public void testConnectWithCorrectCredentials() throws MessagingException {
        imapConnection.connect();
        imapConnection.close();
    }

    @Test(expected = AuthenticationFailedException.class)
    public void testConnectionWithIncorrectCredentials() throws MessagingException {
        IMAPConnection wrongConnection = new IMAPConnection(
                LOCALHOST,
                USER_NAME,
                "",
                Integer.toString(ServerSetup.IMAPS.getPort()),
                "imaps"
        );

        wrongConnection.connect();
        wrongConnection.close();
    }

    @Test
    public void testGetAllFolders() throws IMAPConnectionClosedException, MessagingException {
        imapConnection.connect();

        Folder[] folders = imapConnection.getAllFolders();
        assertEquals(1, folders.length);
        assertEquals("INBOX", folders[0].getFullName());
    }

    @Test
    public void testAddingFolders() throws MessagingException, FolderAlreadyExistsException, IMAPConnectionClosedException {
        imapConnection.connect();

        String[] newFolderNames = {"Test 1", "Test 2", "Test 3"};

        for (String s : newFolderNames) {
            imapConnection.createFolder(s);
        }

        Folder[] folders = imapConnection.getAllFolders();
        assertEquals(1 + newFolderNames.length, folders.length);
        assertEquals("INBOX", folders[0].getFullName());
        for (int i = 0; i < newFolderNames.length; i++) {
            assertEquals(newFolderNames[i], folders[i + 1].getFullName());
        }
    }

    @Test(expected = FolderAlreadyExistsException.class)
    public void testAddingExistingFolder() throws MessagingException, FolderAlreadyExistsException {
        imapConnection.connect();
        imapConnection.createFolder("Test 1");
        imapConnection.createFolder("Test 1");
    }

    @Test(expected = FolderNotFoundException.class)
    public void testNonExistentParentFolder() throws MessagingException, FolderAlreadyExistsException {
        imapConnection.connect();
        imapConnection.createFolder("Test 1");
        Folder f = imapConnection.getFolder("Test 1");
        f.delete(false);
        imapConnection.createFolder(f, "Test 2");
    }

}