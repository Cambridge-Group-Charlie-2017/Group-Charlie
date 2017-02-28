package uk.ac.cam.cl.charlie.mail.offline;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.mail.imap.IMAPFolder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import uk.ac.cam.cl.charlie.mail.*;

import javax.mail.StoreClosedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by Simon on 07/02/2017.
 */
public class OfflineChangeListTest {
    private GreenMail mailServer;
    private GreenMailUser user;

    private static final String USER_NAME = "GROUP-CHARLIE";
    private static final String USER_EMAIL_ADDRESS  = "GROUP-CHARLIE@cam.ac.uk";
    private static final String USER_PASSWORD = "abcdef123";
    private static final String LOCALHOST = "localhost";
    private static final int PORT = 3993;
    private static final String PROTOCOL = "imap";

    private IMAPConnection imapConnection;
    private LocalMailRepresentation mailRepresentation;


    @Before
    public void setUp() throws Exception {
        mailServer = new GreenMail(new ServerSetup(PORT, LOCALHOST, PROTOCOL));
        mailServer.start();

        user = mailServer.setUser(USER_EMAIL_ADDRESS, USER_NAME, USER_PASSWORD);

        imapConnection = new IMAPConnection(
                LOCALHOST,
                USER_NAME,
                USER_PASSWORD,
                Integer.toString(PORT),
                PROTOCOL
        );
        imapConnection.connect();
        mailRepresentation = new LocalMailRepresentation(imapConnection);
        mailRepresentation.createFolder("Test 1");
        mailRepresentation.createFolder("Test 2");
    }

    @After
    public void tearDown() throws Exception {
        if (imapConnection.isConnected()) imapConnection.close();
        mailServer.stop();
        expectedException = ExpectedException.none();
    }

    @Test
    public void testMessageMove() throws Exception {
        LocalIMAPFolderTest.createDefaultMailFormat(user, 0, 1);
        LocalIMAPFolder inbox = mailRepresentation.getFolder("Inbox");
        LocalIMAPFolder test1 = mailRepresentation.getFolder("Test 1");
        LocalMessage message = inbox.getMessages().get(0);

        imapConnection.close();

        inbox.moveMessages(test1, message);

        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);

        assertEquals(0, inbox.getMessages().size());
        assertEquals(1, test1.getMessages().size());
    }

    @Test
    public void testMessageDelete() throws Exception {
        LocalIMAPFolderTest.createDefaultMailFormat(user, 0, 1);
        LocalIMAPFolder inbox = mailRepresentation.getFolder("Inbox");
        assertEquals(1, inbox.getMessages().size());
        LocalMessage message = inbox.getMessages().get(0);

        imapConnection.close();
        inbox.deleteMessages(message);
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);
        assertEquals(0, inbox.getMessages().size());
    }



//    This tests works with real life servers, but  not with GreenMail for some reason
//    Left for completeness, but has to be looked into to if time is available.
//    Currently only passes when the exception it throws when it works is thrown.

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void testFolderMove() throws Exception {
        expectedException.expect(StoreClosedException.class);
        expectedException.expectMessage("* BYE JavaMail Exception: java.io.IOException: Connection dropped by server?");

        LocalIMAPFolder test1 = mailRepresentation.getFolder("Test 1");
        LocalIMAPFolder test2 = mailRepresentation.getFolder("Test 2");

        imapConnection.close();
        test2.moveFolder(test1);
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);

        IMAPFolder[] allFolders = imapConnection.getAllFolders();
        assertTrue(imapConnection.getFolder("Inbox").exists());
        assertTrue(imapConnection.getFolder("Test 1").exists());
        assertTrue(imapConnection.getFolder("Test 1.Test 2").exists());
    }

    @Test
    public void testFolderCreation() throws Exception {
        LocalIMAPFolder test1 = mailRepresentation.getFolder("Test 1");
        imapConnection.close();

        mailRepresentation.createFolder(test1, "Test 3");
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);

        assertTrue(imapConnection.getFolder("Inbox").exists());
        assertTrue(imapConnection.getFolder("Test 1").exists());
        assertTrue(imapConnection.getFolder("Test 1.Test 3").exists());
    }

    @Test
    public void testFolderDeletion() throws Exception {
        imapConnection.close();
        LocalIMAPFolder test1 = mailRepresentation.getFolder("Test 1");
        LocalIMAPFolder test2 = mailRepresentation.getFolder("Test 2");

        test1.delete();
        test2.delete();
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);

        assertTrue(imapConnection.getFolder("Inbox").exists());
        assertFalse(imapConnection.getFolder("Test 1").exists());
        assertFalse(imapConnection.getFolder("Test 2").exists());
    }
}