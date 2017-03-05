package uk.ac.cam.cl.charlie.mail;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * Created by Simon on 21/02/2017.
 */
public class SMTPConnection {

    public class BackedMimeMessage extends MimeMessage {
        private BackedMimeMessage(Session session) {
            super(session);
        }
    }

    private Session session;

    public SMTPConnection(Session s) {
        session = s;
    }

    public SMTPConnection(String host, String username, String password, String port, boolean starttls, boolean ssl) {
        if (ssl && starttls) throw new IllegalArgumentException("Can't have authentication by both StartTLS and SSL.");
        Properties properties = createProperties(host, port, starttls, ssl);
        Authenticator auth = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };

        session = Session.getInstance(properties, auth);
    }

    private static Properties createProperties(String host, String port, boolean starttls, boolean ssl) {
        Properties connectionProperties = new Properties();
        connectionProperties.put("mail.smtp.host", host);
        connectionProperties.put("mail.smtp.port", port);
        if (starttls) {
            connectionProperties.put("mail.smtp.auth", "true");
            connectionProperties.put("mail.smtp.starttls.enable", "true");
        } else if (ssl) {
            connectionProperties.put("mail.smpt.socketFactory.port", port);
            connectionProperties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        }

        return connectionProperties;
    }

    public BackedMimeMessage getEmptyMessage() {
        return new BackedMimeMessage(session);
    }

    public void sendMessage(BackedMimeMessage m) throws MessagingException {
        Transport.send(m);
    }

}
