package uk.ac.cam.cl.charlie.mail;


import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.mail.imap.IMAPFolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.charlie.mail.exceptions.FolderAlreadyExistsException;
import uk.ac.cam.cl.charlie.mail.exceptions.InvalidFolderNameException;

import javax.mail.AuthenticationFailedException;
import javax.mail.FolderNotFoundException;
import javax.mail.NoSuchProviderException;
import javax.mail.StoreClosedException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
    private static final String LOCALHOST = "localhost";
    private static final int PORT = 3993;
    private static final String PROTOCOL = "imap";

    private static IMAPConnection imapConnection;


    @Before
    public void setUp() throws NoSuchProviderException {
        mailServer = new GreenMail(new ServerSetup(PORT, LOCALHOST, PROTOCOL));
        mailServer.start();

        mailServer.setUser(USER_EMAIL_ADDRESS, USER_NAME, USER_PASSWORD);

        imapConnection = new IMAPConnection(
                LOCALHOST,
                USER_NAME,
                USER_PASSWORD,
                Integer.toString(PORT),
                PROTOCOL
        );
    }

    @After
    public void tearDown() {
        mailServer.stop();
    }

    @Test(expected = StoreClosedException.class)
    public void testClosingClosedConnection() throws Exception {
        imapConnection.close();
        imapConnection.close();
    }

    @Test
    public void testConnectWithCorrectCredentials() throws Exception {
        imapConnection.connect();
        imapConnection.close();
    }

    @Test(expected = AuthenticationFailedException.class)
    public void testConnectionWithIncorrectCredentials() throws Exception {
        IMAPConnection wrongConnection = new IMAPConnection(
                LOCALHOST,
                USER_NAME,
                "",
                Integer.toString(PORT),
                PROTOCOL
        );

        wrongConnection.connect();
        wrongConnection.close();
    }

    @Test
    public void testGetAllFolders() throws Exception {
        imapConnection.connect();

        IMAPFolder[] folders = imapConnection.getAllFolders();
        assertEquals(1, folders.length);
        assertEquals("INBOX", folders[0].getFullName());
    }

    @Test
    public void testAddingFolders() throws Exception {
        imapConnection.connect();

        String[] newFolderNames = {"Test 1", "Test 2", "Test 3"};

        for (String s : newFolderNames) {
            imapConnection.createFolder(s);
        }

        IMAPFolder[] folders = imapConnection.getAllFolders();
        assertEquals(1 + newFolderNames.length, folders.length);
        assertEquals("INBOX", folders[0].getFullName());
        for (int i = 0; i < newFolderNames.length; i++) {
            assertEquals(newFolderNames[i], folders[i + 1].getFullName());
        }
    }

    @Test(expected = FolderAlreadyExistsException.class)
    public void testAddingExistingFolder() throws Exception {
        imapConnection.connect();
        imapConnection.createFolder("Test 1");
        imapConnection.createFolder("Test 1");
    }

    @Test(expected = FolderNotFoundException.class)
    public void testNonExistentParentFolder() throws Exception {
        imapConnection.connect();
        imapConnection.createFolder("Test 1");
        IMAPFolder f = imapConnection.getFolder("Test 1");
        f.delete(false);
        imapConnection.createFolder(f, "Test 2");
    }

    @Test
    public void testAddingSubfolders() throws Exception {
        imapConnection.connect();
        imapConnection.createFolder("Test 1");
        imapConnection.createFolder("Test 2");

        IMAPFolder test1 = imapConnection.getFolder("Test 1");
        IMAPFolder test2 = imapConnection.getFolder("Test 2");

        IMAPFolder[] folders = imapConnection.getAllFolders();
        assertEquals(3, folders.length);

        imapConnection.createFolder(test1, "Test 3");
        imapConnection.createFolder(test2, "Test 4");

        folders = imapConnection.getAllFolders();
        assertEquals(5, folders.length);

        assertTrue(Arrays.stream(folders).anyMatch(f -> f.getFullName().equals("Test 1")));
        assertTrue(Arrays.stream(folders).anyMatch(f -> f.getFullName().equals("Test 2")));
        assertTrue(Arrays.stream(folders).anyMatch(f -> f.getFullName().equals("Test 1.Test 3")));
        assertTrue(Arrays.stream(folders).anyMatch(f -> f.getFullName().equals("Test 2.Test 4")));
    }

    @Test(expected = InvalidFolderNameException.class)
    public void testAddingSubfoldersInvalidName() throws Exception {
        imapConnection.connect();
        imapConnection.createFolder("Test 1.1");
    }

}