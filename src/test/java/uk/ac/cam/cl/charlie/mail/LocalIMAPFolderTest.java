package uk.ac.cam.cl.charlie.mail;

import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.mail.imap.IMAPFolder;
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
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Simon on 04/02/2017.
 */
@RunWith(Parameterized.class)
public class LocalIMAPFolderTest {
    @Parameterized.Parameters
    public static Collection magnitudes() {
        return Arrays.asList(new Object[][]{{ 1 },
                { 5 },
                { 10 },
                { 20 }});
    }

    public LocalIMAPFolderTest(int magnitude) {
        this.magnitude = magnitude;
    }

    private final int magnitude;
    private static final String USER_NAME = "GROUP-CHARLIE";

    private static final String USER_EMAIL_ADDRESS  = "GROUP-CHARLIE@cam.ac.uk";
    private static final String USER_PASSWORD = "abcdef123";
    private static final String LOCALHOST = "localhost";
    private static final int PORT = 3993;
    private static final String PROTOCOL = "imap";

    private GreenMail mailServer;
    private GreenMailUser user;
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
    }

    @After
    public void tearDown() {
        mailServer.stop();
    }

    @Test
    public void testRetrievingMail() throws Exception {
        createDefaultMailFormat(user, 0, 5 * magnitude);
        LocalIMAPFolder localFolder = mailRepresentation.getFolder("Inbox");
        checkDefaultMailFormat(localFolder.getMessages(), 0, 5 * magnitude);

        createDefaultMailFormat(user, 5 * magnitude, 10 * magnitude);
        localFolder.sync();
        checkDefaultMailFormat(localFolder.getMessages(), 0, 10 * magnitude);
    }

    @Test
    public void testRetrievingMailAfterMoving() throws Exception {
        mailRepresentation.createFolder("Test 1");
        createDefaultMailFormat(user, 0, 10 * magnitude);

        LocalIMAPFolder localFolder = mailRepresentation.getFolder("Inbox");
        LocalMessage[] toBeMoved = new LocalMessage[2 * magnitude];
        localFolder.getMessages().subList(2 * magnitude, 4 * magnitude).toArray(toBeMoved);
        localFolder.moveMessages(mailRepresentation.getFolder("Test 1"), toBeMoved);
        localFolder.sync();

        Integer[] toBeMovedIndexes = new Integer[2 * magnitude];
        for (int i = 2 * magnitude; i < 4 * magnitude; i++) {
            toBeMovedIndexes[i - 2 * magnitude] = i;
        }

        localFolder.sync();
        assertEquals(8 * magnitude, localFolder.getMessages().size());
        checkDefaultMailFormat(localFolder.getMessages(), 0, 10 * magnitude, toBeMovedIndexes);
    }

    @Test
    public void testMovingMail() throws Exception {
        mailRepresentation.createFolder("Test 1");
        createDefaultMailFormat(user, 0, 10 * magnitude);

        LocalIMAPFolder localFolder = mailRepresentation.getFolder("Inbox");
        LocalMessage[] toBeMoved = new LocalMessage[2 * magnitude];
        localFolder.getMessages().subList(2 * magnitude, 4 * magnitude).toArray(toBeMoved);
        localFolder.moveMessages(mailRepresentation.getFolder("Test 1"), toBeMoved);
        localFolder.sync();

        assertEquals(2 * magnitude, mailRepresentation.getFolder("Test 1").getMessages().size());
        checkDefaultMailFormat(mailRepresentation.getFolder("Test 1").getMessages(), 2 * magnitude, 4 * magnitude);
    }

    @Test
    public void testDeletingMail() throws Exception {
        createDefaultMailFormat(user, 0, 10);

        LocalIMAPFolder localFolder = mailRepresentation.getFolder("Inbox");
        localFolder.deleteMessages(localFolder.getMessages().get(1), localFolder.getMessages().get(2));
        localFolder.sync();

        assertEquals(8, mailRepresentation.getFolder("Inbox").getMessages().size());
        checkDefaultMailFormat(localFolder.getMessages(), 0, 10, 1, 2);
    }

    @Test
    public void testReconnectToFolder() throws Exception {
        LocalIMAPFolder inbox = mailRepresentation.getFolder("Inbox");
        for (int i = 0; i < magnitude; i++) {
            mailRepresentation.createFolder("Test " + i);
        }
        imapConnection.close();
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);

        assertEquals(0, inbox.getMessages().size());
        for (int i = 0; i < magnitude; i++) {
            assertEquals(0, mailRepresentation.getFolder("Test " + i).getMessages().size());
        }
    }

    @Test
    public void testSynchingAfterReconnect() throws Exception {
        LocalIMAPFolder inbox = mailRepresentation.getFolder("Inbox");
        assertEquals(0, inbox.getMessages().size());
        imapConnection.close();
        createDefaultMailFormat(user, 0, magnitude);
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);

        checkDefaultMailFormat(inbox.getMessages(), 0, magnitude);
    }

    @Test
    public void testMovingFolder() throws Exception {
        LocalIMAPFolder inbox = mailRepresentation.getFolder("Inbox");
        LocalIMAPFolder test1 = mailRepresentation.createFolder("Test 1");
        LocalIMAPFolder test2 = mailRepresentation.createFolder("Test 2");

        test2.moveFolder(test1);

        assertTrue(imapConnection.getFolder("Inbox").exists());
        assertTrue(imapConnection.getFolder("Test 1").exists());
        assertTrue(imapConnection.getFolder("Test 1.Test 2").exists());

        test1.moveFolder(inbox);

        assertTrue(imapConnection.getFolder("Inbox").exists());
        assertTrue(imapConnection.getFolder("Inbox.Test 1").exists());
        assertTrue(imapConnection.getFolder("Inbox.Test 1.Test 2").exists());

    }

    @Test
    public void testMovingMessages() throws Exception {
        createDefaultMailFormat(user, 0, 5);
        LocalIMAPFolder inbox = mailRepresentation.getFolder("Inbox");
        LocalIMAPFolder test1 = mailRepresentation.createFolder("Test 1");
        LocalIMAPFolder test2 = mailRepresentation.createFolder("Test 2");
        LocalMessage message1 = inbox.getMessages().get(0);
        LocalMessage message2 = inbox.getMessages().get(1);

        inbox.moveMessages(test1, message1);

        assertEquals(4, inbox.getMessages().size());
        assertEquals(1, test1.getMessages().size());
        assertEquals(0, test2.getMessages().size());
        assertEquals(4, inbox.getBackingFolder().getMessageCount());
        assertEquals(1, test1.getBackingFolder().getMessageCount());
        assertEquals(0, test2.getBackingFolder().getMessageCount());
        
        imapConnection.close();
        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);

        inbox.moveMessages(test2, message2);

        assertEquals(3, inbox.getMessages().size());
        assertEquals(1, test1.getMessages().size());
        assertEquals(1, test2.getMessages().size());
        assertEquals(3, inbox.getBackingFolder().getMessageCount());
        assertEquals(1, test1.getBackingFolder().getMessageCount());
        assertEquals(1, test2.getBackingFolder().getMessageCount());


    }

    @Test
    public void testServersideFolderCreation() throws Exception {
        mailServer.getManagers().getImapHostManager().createMailbox(user, "Test 1");
        mailRepresentation.syncAllFolders();
        mailRepresentation.getFolder("Test 1");
    }

    @Test
    public void testServersideFolderCreationMessages() throws Exception {
        MailFolder serverFolder = mailServer.getManagers().getImapHostManager().createMailbox(user, "Test 1");
        for (int i = 0; i < magnitude; i++) {
            serverFolder.appendMessage(createFakeMail("from" + i, "to" + i, "subject " + i, "content " + i), new Flags(Flags.Flag.RECENT), new Date());
        }
        mailRepresentation.syncAllFolders();
        LocalIMAPFolder test3 = mailRepresentation.getFolder("Test 1");
        assertEquals(magnitude, test3.getMessages().size());
    }

    @Test(expected = FolderNotFoundException.class)
    public void testServersideFolderDeletion() throws Exception {
        mailRepresentation.createFolder("Test 1");
        mailServer.getManagers().getImapHostManager().deleteMailbox(user, "Test 1");
        mailRepresentation.syncAllFolders();
        mailRepresentation.getFolder("Test 1");
    }

    @Test
    public void testServersideNewMessages() throws Exception {
        createDefaultMailFormat(user, 0, magnitude * 5);
        mailRepresentation.syncAllFolders();

        LocalIMAPFolder inbox = mailRepresentation.getFolder("Inbox");
        assertEquals(magnitude * 5, inbox.getMessages().size());

        MailFolder serverFolder = mailServer.getManagers().getImapHostManager().getFolder(user, "Inbox");
        for (int i = magnitude * 5; i < magnitude * 10; i++) {
            serverFolder.appendMessage(createFakeMail("from" + i, "to" + i, "subject " + i, "content " + i), new Flags(Flags.Flag.RECENT), new Date());
        }
        mailRepresentation.syncAllFolders();

        checkDefaultMailFormat(inbox.getMessages(), 0, magnitude * 10);
    }

    @Test
    public void testServersideDeleteMessage() throws Exception {
        createDefaultMailFormat(user, 0, magnitude * 10);
        MailFolder serverFolder = mailServer.getManagers().getImapHostManager().getFolder(user, "Inbox");
        for (int i = 0; i < magnitude * 5; i++) {
            serverFolder.getMessages().get(i).setFlag(Flags.Flag.DELETED, true);
        }
        serverFolder.expunge();

        mailRepresentation.syncAllFolders();

        LocalIMAPFolder inbox = mailRepresentation.getFolder("Inbox");
        checkDefaultMailFormat(inbox.getMessages(), 5 * magnitude, 10 * magnitude);
    }

    public static void createDefaultMailFormat(GreenMailUser user, int start, int end) throws MessagingException {
        for (int i = start; i < end; i++) {
            deliverFakeMail(user, "from" + i, "to" + i, "subject " + i, "content " + i);
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

    public static void deliverFakeMail(GreenMailUser user, String from, String to, String subject, String content) throws MessagingException {
        user.deliver(createFakeMail(from, to, subject, content));
    }

    public static MimeMessage createFakeMail(String from, String to, String subject, String content) throws MessagingException {
        MimeMessage message = new MimeMessage((Session) null);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(content);
        return message;
    }

}