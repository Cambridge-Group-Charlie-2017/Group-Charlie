package uk.ac.cam.cl.charlie.mail;

import java.io.IOException;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;

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

}
