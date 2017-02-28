package uk.ac.cam.cl.charlie.mail;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.jsoup.Jsoup;

import com.sun.mail.imap.IMAPMessage;

import uk.ac.cam.cl.charlie.mail.sync.SyncIMAPMessage;

/**
 * Helpers for easy accessing {@link Message}
 *
 * @author Gary Guo
 *
 */
public class Messages {

    /**
     * Get the body part of a message.
     *
     * @param part
     *            the root message
     * @param preferHTML
     *            determine whether HTML or plain text is preferred
     * @return the body part containing the text message
     * @throws MessagingException
     * @throws IOException
     */
    public static Part getBodyPart(Part part, boolean preferHTML) throws MessagingException, IOException {
        // Return as is if it is text already
        if (part.isMimeType("text/*")) {
            return part;
        }

        if (part.isMimeType("multipart/alternative")) {
            // multipart/alternative usually include both html and plain text
            // email
            Multipart mp = (Multipart) part.getContent();
            Part ret = null;

            for (int i = 0; i < mp.getCount(); i++) {
                Part bp = mp.getBodyPart(i);
                if (preferHTML ? bp.isMimeType("text/html") : bp.isMimeType("text/plain")) {
                    Part s = getBodyPart(bp, preferHTML);
                    if (s != null)
                        return s;
                } else if (bp.isMimeType("text/*")) {
                    if (ret == null)
                        ret = getBodyPart(bp, preferHTML);
                    continue;
                }
            }

            return ret;
        } else if (part.isMimeType("multipart/*")) {
            Multipart mp = (Multipart) part.getContent();
            for (int i = 0; i < mp.getCount(); i++) {
                Part s = getBodyPart(mp.getBodyPart(i), preferHTML);
                if (s != null)
                    return s;
            }
        }

        return null;
    }

    /**
     * Get the body text of a message. If the message contains only HTML
     * message, it will be parsed and converted to plain texts.
     *
     * @param part
     *            the root message
     * @return the body text message
     * @throws MessagingException
     * @throws IOException
     */
    public static String getBodyText(Part part) throws MessagingException, IOException {
        Part p = getBodyPart(part, false);
        if (p == null)
            return null;
        if (p.isMimeType("text/html")) {
            return Jsoup.parse((String) p.getContent()).text();
        }
        return (String) p.getContent();
    }

    public static InternetAddress getFromAddress(Message msg) throws MessagingException {
        return (InternetAddress) msg.getFrom()[0];
    }

    public static String getInReplyTo(Message message) throws MessagingException {
        if (message instanceof IMAPMessage) {
            return ((IMAPMessage) message).getInReplyTo();
        } else {
            String[] headers = message.getHeader("In-Reply-To");
            if (headers == null)
                return null;
            if (headers.length != 0)
                return headers[0];
        }
        return null;
    }

    public static Object fastGetContent(Message message) throws MessagingException, IOException {
        if (message instanceof SyncIMAPMessage) {
            return ((SyncIMAPMessage) message).fastGetContent();
        }
        return null;
    }

    public static String getMessageID(Message message) throws MessagingException {
        if (message instanceof MimeMessage) {
            return ((MimeMessage) message).getMessageID();
        } else {
            return null;
        }
    }
}
