package uk.ac.cam.cl.charlie.mail.offline;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.cam.cl.charlie.mail.IMAPConnection;
import uk.ac.cam.cl.charlie.mail.LocalIMAPFolder;
import uk.ac.cam.cl.charlie.mail.LocalIMAPFolderTest;
import uk.ac.cam.cl.charlie.mail.LocalMessage;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import static org.junit.Assert.*;

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

    private static IMAPConnection imapConnection;
    private static OfflineChangeList offlineChangeList;


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
        imapConnection.createFolder("Test 1");
        imapConnection.createFolder("Test 2");
        imapConnection.close();
        offlineChangeList = OfflineChangeList.getInstance();
        offlineChangeList.clearChanges();
    }

    @After
    public void tearDown() throws Exception {
        if (imapConnection.isConnected()) imapConnection.close();
    }

    @Test
    public void testMessageMove() throws Exception {
        imapConnection.connect();
        LocalIMAPFolderTest.createDefaultMailFormat(user, 0, 1);
        LocalIMAPFolder inbox = new LocalIMAPFolder(imapConnection, imapConnection.getFolder("Inbox"));
        LocalIMAPFolder test1 = new LocalIMAPFolder(imapConnection, imapConnection.getFolder("Test 1"));
        LocalMessage message = inbox.getMessages().get(0);

        imapConnection.close();
        offlineChangeList.addChange(new MessageMove(inbox, test1, message));
        imapConnection.connect();
        inbox.openConnection(imapConnection);
        test1.openConnection(imapConnection);
        offlineChangeList.performChanges(imapConnection);
        inbox.sync();
        test1.sync();

        assertEquals(0, inbox.getMessages().size());
        assertEquals(1, test1.getMessages().size());
    }

}