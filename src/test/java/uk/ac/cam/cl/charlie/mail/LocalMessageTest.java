package uk.ac.cam.cl.charlie.mail;

import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.mail.NoSuchProviderException;

import static org.junit.Assert.assertEquals;

/**
 * Created by Simon on 09/02/2017.
 */
public class LocalMessageTest {
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
    }

    @After
    public void tearDown() {
        mailServer.stop();
    }

    @Test
    public void testCorrectMessageAfterReconnect() throws Exception {
        LocalIMAPFolderTest.createDefaultMailFormat(user, 0, 1);
        LocalIMAPFolder localFolder = mailRepresentation.getFolder("Inbox");
        LocalMessage message = localFolder.getMessages().get(0);
        assertEquals(true, message.hasConnection());

        imapConnection.close();
        assertEquals(false, message.hasConnection());

        imapConnection.connect();
        mailRepresentation.setConnection(imapConnection);
        assertEquals(true, message.hasConnection());
    }

}