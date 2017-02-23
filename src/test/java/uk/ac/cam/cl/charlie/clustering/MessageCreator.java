package uk.ac.cam.cl.charlie.clustering;

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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Matthew Boyce
 */
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
            for(File file : attachments) {
                MimeBodyPart attachment = new MimeBodyPart();
                DataSource source = new FileDataSource(file);
                attachment.setDataHandler(new DataHandler(source));
                attachment.setFileName(file.getName());
                multipart.addBodyPart(attachment);
            }
            // integration
            message.setContent(multipart);
            message.saveChanges();
            // store file
            return message;
        } catch (MessagingException e) {
            System.out.println(e.getMessage());
        }
        return null;
    }

    public static ArrayList<Message> createTestMessages(){
        ArrayList<Message> messages= new ArrayList<Message>();
        ArrayList<File> files = new ArrayList<File>();
        messages.add(createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));
        messages.add(createMessage("blabla@gmail.com","Bob@companyname.com","Project X", "",files));

        return messages;
    }
}
