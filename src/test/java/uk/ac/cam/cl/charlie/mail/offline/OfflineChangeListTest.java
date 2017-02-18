package uk.ac.cam.cl.charlie.mail.offline;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.charlie.mail.*;

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
    }

    @Test
    public void testMessageMove() throws Exception {
        LocalIMAPFolderTest.createDefaultMailFormat(user, 0, 1);
        LocalIMAPFolder inbox = mailRepresentation.getFolder("Inbox");
        LocalIMAPFolder test1 = mailRepresentation.getFolder("Test 1");
        LocalMessage message = inbox.getMessages().get(0);

        imapConnection.close();

        OfflineChangeList.getInstance().addChange(new MessageMove(inbox, test1, message));
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);
        inbox.sync();
        test1.sync();

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
        OfflineChangeList.getInstance().addChange(new MessageDelete(inbox, message));
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);
        assertEquals(0, inbox.getMessages().size());
    }

    @Test
    public void testFolderMove() throws Exception {
        LocalIMAPFolder test1 = mailRepresentation.getFolder("Test 1");
        LocalIMAPFolder test2 = mailRepresentation.getFolder("Test 2");

        imapConnection.close();
        OfflineChangeList.getInstance().addChange(new FolderMove(test2.getFullName(), test1.getFullName()));
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);

        assertTrue(imapConnection.getFolder("Inbox").exists());
        assertTrue(imapConnection.getFolder("Test 1").exists());
        assertTrue(imapConnection.getFolder("Test 1.Test 2").exists());
    }

    @Test
    public void testFolderCreation() throws Exception {
        imapConnection.close();
        LocalIMAPFolder test1 = mailRepresentation.getFolder("Test 1");

        OfflineChangeList.getInstance().addChange(new FolderCreation(test1, "Test 3"));
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

        OfflineChangeList.getInstance().addChange(new FolderDeletion(test1));
        OfflineChangeList.getInstance().addChange(new FolderDeletion(test2));
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);

        assertTrue(imapConnection.getFolder("Inbox").exists());
        assertFalse(imapConnection.getFolder("Test 1").exists());
        assertFalse(imapConnection.getFolder("Test 2").exists());
    }
}