package uk.ac.cam.cl.charlie.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimePart;

import spark.utils.StringUtils;
import uk.ac.cam.cl.charlie.util.IntHolder;

public class MailUtil {

    public static List<Part> getAttachments(Message message) throws Exception {
        Object content = message.getContent();
        if (content instanceof String)
            return new ArrayList<>();

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            List<Part> result = new ArrayList<>();

            for (int i = 0; i < multipart.getCount(); i++) {
                result.addAll(getAttachments(multipart.getBodyPart(i)));
            }
            return result;
        }
        return new ArrayList<>();
    }

    private static List<Part> getAttachments(BodyPart part) throws Exception {
        List<Part> result = new ArrayList<>();
        Object content = part.getContent();
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
                || Part.INLINE.equalsIgnoreCase(part.getDisposition()) || StringUtils.isNotBlank(part.getFileName())) {
            result.add(part);
            return result;
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                result.addAll(getAttachments(bodyPart));
            }
            return result;
        } else {
            return new ArrayList<>();
        }
    }

    public static InputStream searchAttachments(Part part, String cid) throws MessagingException, IOException {
        Object content = part.getContent();
        if (content instanceof InputStream || content instanceof String) {
            if (part instanceof MimePart) {
                MimePart mimePart = (MimePart) part;
                String cidstring = mimePart.getContentID();
                if (cidstring == null) {
                    return null;
                }
                try {
                    InternetAddress addr = new InternetAddress(cidstring);
                    if (addr.getAddress().equals(cid)) {
                        return part.getInputStream();
                    }
                } catch (AddressException e) {
                    // addr could be malformed
                }
            }
        }

        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                InputStream stream = searchAttachments(bodyPart, cid);
                if (stream != null)
                    return stream;
            }
        }
        return null;
    }

    public static Part searchAttachmentsByName(Part part, String name) throws MessagingException, IOException {
        Object content = part.getContent();
        if (StringUtils.isNotBlank(part.getFileName())) {
            if (part.getFileName().equals(name)) {
                return part;
            }
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                Part result = searchAttachmentsByName(multipart.getBodyPart(i), name);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

    public static Part searchAttachmentsByIndex(Part part, int targetIndex, IntHolder index)
            throws MessagingException, IOException {
        Object content = part.getContent();
        if (Part.ATTACHMENT.equalsIgnoreCase(part.getDisposition())
                || Part.INLINE.equalsIgnoreCase(part.getDisposition()) || StringUtils.isNotBlank(part.getFileName())) {
            if (targetIndex == index.value)
                return part;
            index.value++;
            return null;
        } else if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            for (int i = 0; i < multipart.getCount(); i++) {
                Part result = searchAttachmentsByIndex(multipart.getBodyPart(i), targetIndex, index);
                if (result != null)
                    return result;
            }
        }
        return null;
    }

}
