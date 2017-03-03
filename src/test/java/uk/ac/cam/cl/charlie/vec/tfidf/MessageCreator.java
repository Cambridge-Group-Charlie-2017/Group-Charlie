package uk.ac.cam.cl.charlie.vec.tfidf;

import java.io.File;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 * Created by Ben on 12/02/2017.
 * 
 * @author Matthew Boyce
 */

// Class for creating javax.mail.Message objects for testing purposes.

public class MessageCreator {
    public static Message createMessage(String to, String from, String subject, String body, List<File> attachments) {
        try {
            Message message = new MimeMessage(Session.getInstance(System.getProperties()));
            message.setFrom(new InternetAddress(from));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            // create the message part
            MimeBodyPart content = new MimeBodyPart();
            // fill message
            content.setText(body);
            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(content);
            // add attachments
            for (File file : attachments) {
                MimeBodyPart attachment = new MimeBodyPart();
                DataSource source = new FileDataSource(file);
                attachment.setDataHandler(new DataHandler(source));
                attachment.setFileName(file.getName());
                multipart.addBodyPart(attachment);
            }
            // integration
            message.setContent(multipart);
            // store file
            return message;
        } catch (MessagingException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }
}