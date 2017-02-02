import java.util.Date;

/**
 * Created by Ben on 01/02/2017.
 */

//NOTE: Not sure if Email class is actually useful. We could pass emails by ID instead, keeping them on database.
public class Email {
    Date dateReceived;
    String header; //includes metadata
    String subject;
    String messageBody;
    String[] attachments; //array of strings containing file paths of attachments.

    String getBody() {return messageBody;}
    String getHeader() {return header;}
    String getSubject() {return subject;}
    Date getData() {return dateReceived;}
}
