package uk.ac.cam.cl.charlie.mail;

import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import uk.ac.cam.cl.charlie.mail.SMTPConnection.BackedMimeMessage;

import static org.junit.Assert.*;

/**
 * Created by Simon on 22/02/2017.
 */
public class SMTPConnectionTest {
    private GreenMail mailServer;
    private static final String SENDING_USER_NAME = "GROUP_CHARLIE_SENDING";
    private static final String SENDING_USER_EMAIL_ADDRESS  = "group_charlie_sending@example.com";
    // DO NOT USE UPPER CASE LETTERS FOR THE EMAIL ADDRESS, OTHERWISE THE TEST 'sendMessage' WILL FAIL.
    private static final String RECEIVING_USER_EMAIL_ADDRES = "group_charlie_sending@example.com";
    private static final String USER_PASSWORD = "abcdef123";

    private SMTPConnection smtpConnection;


    @Before
    public void setUp() throws MessagingException {
        mailServer = new GreenMail(ServerSetupTest.SMTP_IMAP);
        mailServer.start();

        mailServer.setUser(SENDING_USER_EMAIL_ADDRESS, SENDING_USER_NAME, USER_PASSWORD);

        smtpConnection = new SMTPConnection(
                mailServer.getSmtp().createSession()
        );
    }

    @After
    public void tearDown() throws MessagingException {
        mailServer.stop();
    }

    @Test
    public void getEmptyMessage() throws Exception {
        BackedMimeMessage backedMimeMessage = smtpConnection.getEmptyMessage();
        assertNull(backedMimeMessage.getAllRecipients());
        assertNull(backedMimeMessage.getFrom());
        assertNull(backedMimeMessage.getSubject());
        assertEquals("text/plain", backedMimeMessage.getContentType());
        assertNull(backedMimeMessage.getDescription());
        assertNull(backedMimeMessage.getFolder());
        assertEquals(-1, backedMimeMessage.getSize());
        assertNull(backedMimeMessage.getMessageID());
        assertNull(backedMimeMessage.getReceivedDate());
        assertFalse(backedMimeMessage.getAllHeaders().hasMoreElements());
        assertEquals(0, backedMimeMessage.getMessageNumber());
        assertNull(backedMimeMessage.getEncoding());
    }

    @Test
    public void sendMessage() throws Exception {
        BackedMimeMessage backedMimeMessage = smtpConnection.getEmptyMessage();
        backedMimeMessage.setFrom(new InternetAddress(SENDING_USER_EMAIL_ADDRESS));
        backedMimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(RECEIVING_USER_EMAIL_ADDRES));
        backedMimeMessage.setText("Test 1");
        backedMimeMessage.setSubject("Subject 1");

        smtpConnection.sendMessage(backedMimeMessage);

        assertEquals(1, mailServer.getReceivedMessagesForDomain(RECEIVING_USER_EMAIL_ADDRES).length);
        Message receivedMessage = mailServer.getReceivedMessagesForDomain(RECEIVING_USER_EMAIL_ADDRES)[0];
        assertEquals(backedMimeMessage.getSubject(), receivedMessage.getSubject());
        
        // received message has extra \r\n
        assertEquals(backedMimeMessage.getContent().toString().trim(), receivedMessage.getContent().toString().trim());
    }

}