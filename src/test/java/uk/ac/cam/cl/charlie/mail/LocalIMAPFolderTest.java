package uk.ac.cam.cl.charlie.mail;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by Simon on 04/02/2017.
 */
@RunWith(Parameterized.class)
public class LocalIMAPFolderTest {
    @Parameterized.Parameters
    public static Collection primeNumbers() {
        return Arrays.asList(new Object[][] {
                { 1 },
                { 5 },
                { 10 },
                { 20 }
        });
    }

    public LocalIMAPFolderTest(int magnitude) {
        this.magnitude = magnitude;
    }




    private GreenMail mailServer;
    private GreenMailUser user;
    private final int magnitude;

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
        user = mailServer.setUser(USER_EMAIL_ADDRESS, USER_NAME, USER_PASSWORD);

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

    @Test
    public void testRetrievingMail() throws Exception {
        imapConnection.connect();
        createDefaultMailFormat(user, 0, 5 * magnitude);
        LocalIMAPFolder localFolder = new LocalIMAPFolder(imapConnection, imapConnection.getFolder("Inbox"));
        checkDefaultMailFormat(localFolder.getMessages(), 0, 5 * magnitude);

        createDefaultMailFormat(user, 5 * magnitude, 10 * magnitude);
        localFolder.sync();
        checkDefaultMailFormat(localFolder.getMessages(), 0, 10 * magnitude);
    }

    @Test
    public void testRetrievingMailAfterMoving() throws Exception {
        imapConnection.connect();
        imapConnection.createFolder("Test 1");
        createDefaultMailFormat(user, 0, 10 * magnitude);

        LocalIMAPFolder localFolder = new LocalIMAPFolder(imapConnection, imapConnection.getFolder("Inbox"));
        LocalMessage[] toBeMoved = new LocalMessage[2 * magnitude];
        localFolder.getMessages().subList(2 * magnitude, 4 * magnitude).toArray(toBeMoved);
        localFolder.moveMessages(imapConnection.getFolder("Test 1"), toBeMoved);
        localFolder.sync();

        Integer[] toBeMovedIndexes = new Integer[2 * magnitude];
        for (int i = 2 * magnitude; i < 4 * magnitude; i++) {
            toBeMovedIndexes[i - 2 * magnitude] = i;
        }
        assertEquals(8 * magnitude, imapConnection.getFolder("Inbox").getMessageCount());
        checkDefaultMailFormat(localFolder.getMessages(), 0, 10 * magnitude, toBeMovedIndexes);
    }

    @Test
    public void testMovingMail() throws Exception {
        imapConnection.connect();
        imapConnection.createFolder("Test 1");
        createDefaultMailFormat(user, 0, 10 * magnitude);

        LocalIMAPFolder localFolder = new LocalIMAPFolder(imapConnection, imapConnection.getFolder("Inbox"));
        LocalMessage[] toBeMoved = new LocalMessage[2 * magnitude];
        localFolder.getMessages().subList(2 * magnitude, 4 * magnitude).toArray(toBeMoved);
        localFolder.moveMessages(imapConnection.getFolder("Test 1"), toBeMoved);
        localFolder.sync();

        assertEquals(2 * magnitude, imapConnection.getFolder("Test 1").getMessageCount());
        checkDefaultMailFormat(new LocalIMAPFolder(imapConnection, imapConnection.getFolder("Test 1")).getMessages(), 2 * magnitude, 4 * magnitude);
    }

    @Test
    public void testDeletingMail() throws Exception {
        imapConnection.connect();
        createDefaultMailFormat(user, 0, 10);

        LocalIMAPFolder localFolder = new LocalIMAPFolder(imapConnection, imapConnection.getFolder("Inbox"));
        localFolder.deleteMessages(localFolder.getMessages().get(1), localFolder.getMessages().get(2));
        localFolder.sync();

        assertEquals(8, imapConnection.getFolder("Inbox").getMessageCount());
        checkDefaultMailFormat(localFolder.getMessages(), 0, 10, 1, 2);
    }

    @Test
    public void testReconnectToFolder() throws Exception {
        imapConnection.connect();
        LocalIMAPFolder inbox = new LocalIMAPFolder(imapConnection, imapConnection.getFolder("Inbox"));
        LocalIMAPFolder[] testFolders = new LocalIMAPFolder[magnitude];
        for (int i = 0; i < magnitude; i++) {
            imapConnection.createFolder("Test " + i);
            testFolders[i] = new LocalIMAPFolder(imapConnection, imapConnection.getFolder("Test " + i));
        }
        imapConnection.close();
        imapConnection.connect();
        inbox.openConnection(imapConnection);
        inbox.sync();
        for (LocalIMAPFolder m : testFolders) {
            m.openConnection(imapConnection);
            m.sync();
        }

        assertEquals(0, inbox.getMessages().size());
        for (int i = 0; i < magnitude; i++) {
            assertEquals(0, testFolders[0].getMessages().size());
        }
    }

    @Test
    public void testSynchingAfterReconnect() throws Exception {
        imapConnection.connect();
        LocalIMAPFolder inbox = new LocalIMAPFolder(imapConnection, imapConnection.getFolder("Inbox"));

        inbox.sync();
        assertEquals(0, inbox.getMessages().size());
        imapConnection.close();
        createDefaultMailFormat(user, 0, magnitude);
        imapConnection.connect();
        inbox.openConnection(imapConnection);
        assertEquals(0, inbox.getMessages().size());
        inbox.sync();

        checkDefaultMailFormat(inbox.getMessages(), 0, magnitude);
    }

    public static void createDefaultMailFormat(GreenMailUser user, int start, int end) throws MessagingException {
        for (int i = start; i < end; i++) {
            createFakeEmail(user, "from" + i, "to" + i, "subject " + i, "content " + i);
        }
    }

    public static void checkDefaultMailFormat(List<LocalMessage> messages, int startMessageNumber, int endMessageNumberExclusive, Integer... exclusions) throws Exception {
        int skippedCount = 0;
        int messageNumber = 0;
        outerloop:
        for (int i = 0; i < messages.size(); i++) {
            for (Integer exc : exclusions) {
                if (exc.intValue() == i) {
                    skippedCount++;
                    continue outerloop;
                }
            }
            LocalMessage m = messages.get(i);
            messageNumber = startMessageNumber + i + skippedCount;

            assertEquals("content " + messageNumber, m.getContent());
            assertEquals("subject " + messageNumber, m.getSubject());
            assertEquals(1, m.getRecipients().length);
            assertEquals("to" + messageNumber + "@localhost", m.getRecipients()[0].toString());
            assertEquals(1, m.getFrom().length);
            assertEquals("from" + messageNumber + "@localhost", m.getFrom()[0].toString());
        }
        assertEquals(endMessageNumberExclusive - 1, messageNumber);
    }

    public static void createFakeEmail(GreenMailUser user, String from, String to, String subject, String content) throws MessagingException {
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(content);
        user.deliver(message);
    }

}