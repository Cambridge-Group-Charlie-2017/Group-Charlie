package uk.ac.cam.cl.charlie.demo;

import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetupTest;
import com.pff.PSTException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by Simon on 28/02/2017.
 */
public class LocalGreenMailServer {

    public static void setupGreenMailWithEnron(String host, String emailaddress, String username, String password) throws PSTException, IOException, MessagingException {
        GreenMail greenMail = new GreenMail(ServerSetupTest.IMAP);
        greenMail.start();
        GreenMailUser user = greenMail.setUser(emailaddress, username, password);

        final MailFolder inbox = greenMail.getManagers().getImapHostManager().getFolder(user, "Inbox");

        PSTProcessor pstProcessor = new PSTProcessor();
        final ArrayList<MimeMessage> messages = pstProcessor.processAllEnron();

        for (MimeMessage m : messages) {
            inbox.appendMessage(m, m.getFlags(), m.getReceivedDate());
        }
    }
}
