package uk.ac.cam.cl.charlie.ui;

import static spark.Spark.after;
import static spark.Spark.get;
import static spark.Spark.ipAddress;
import static spark.Spark.options;
import static spark.Spark.patch;
import static spark.Spark.port;
import static spark.Spark.post;
import static spark.Spark.put;
import static spark.Spark.staticFiles;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import spark.Request;
import spark.Response;
import spark.Spark;
import spark.utils.StringUtils;
import uk.ac.cam.cl.charlie.mail.Messages;
import uk.ac.cam.cl.charlie.util.IOUtils;
import uk.ac.cam.cl.charlie.util.IntHolder;
import uk.ac.cam.cl.charlie.util.Wallpaper;

public class WebUIServer {

    public static final int PORT = 6245;

    private static Logger log = LoggerFactory.getLogger(WebUIServer.class);
    private static WebUIServer instance;

    private Client client = Client.getInstance();

    private static HashSet<String> directOpenExtension = new HashSet<>();
    static {
        // Pictures
        directOpenExtension.add(".png");
        directOpenExtension.add(".gif");
        directOpenExtension.add(".jpg");

        // Documents
        directOpenExtension.add(".doc");
        directOpenExtension.add(".docx");
        directOpenExtension.add(".pdf");
        directOpenExtension.add(".txt");
    }

    public static WebUIServer getInstance() {
        if (instance == null) {
            instance = new WebUIServer();
        }
        return instance;
    }

    private static JsonObject serializeContact(InternetAddress address) {
        JsonObject ret = new JsonObject();
        ret.addProperty("name", address.getPersonal());
        ret.addProperty("address", address.getAddress());
        return ret;
    }

    private static JsonObject serializeContact(Address address) {
        if (address instanceof InternetAddress) {
            return serializeContact((InternetAddress) address);
        }
        return null;
    }

    private static JsonArray serializeContactCollection(Address[] address) {
        if (address == null) {
            return new JsonArray();
        }
        JsonArray array = new JsonArray();
        for (Address addr : address) {
            JsonObject obj = serializeContact(addr);
            if (obj != null)
                array.add(obj);
        }
        return array;
    }

    private Object resourcesBackground(Request req, Response res) throws IOException {
        String wallpaper = Wallpaper.getWallpaper();
        if (wallpaper == null) {
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("wallpaper.jpg")) {
                res.type("image/jpeg");
                return IOUtils.readBytes(is);
            }
        }
        try (InputStream is = new BufferedInputStream(new FileInputStream(wallpaper))) {
            res.type(URLConnection.guessContentTypeFromStream(is));
            return IOUtils.readBytes(is);
        }
    }

    private WebUIServer() {
        // For security consideration, we limit connection to localhost
        ipAddress("127.0.0.1");
        port(6245);

        staticFiles.externalLocation(System.getProperty("user.dir") + "/src/main/resources/public");
        // staticFileLocation("/public");

        // Serve as the delegate of some operation initiated by the server
        get("/api/native/select_file", (request, response) -> {
            FileChooser chooser = new FileChooser();
            String title = request.queryParams("title");
            if (title != null) {
                chooser.title(title);
            }
            String file = request.queryParams("file");
            if (file != null) {
                chooser.file(file);
            }
            File selected = chooser.open();
            if (selected == null)
                return "null";
            return new JsonPrimitive(selected.getAbsolutePath()).toString();
        });

        get("/api/native/select_folder", (request, response) -> {
            File file = new DirectoryChooser().open();
            if (file == null)
                return "null";
            return new JsonPrimitive(file.getAbsolutePath()).toString();
        });

        get("/api/settings/config/:key", (request, response) -> {
            String key = request.params("key");
            try {
                String value = Client.getInstance().getConfiguration(key);
                if (value == null)
                    return "null";
                if (key.contains("password")) {
                    return "\"(password)\"";
                }
                return new JsonPrimitive(value).toString();
            } catch (RuntimeException e) {
                response.status(404);
                return e.getMessage();
            }
        });

        options("/api/settings/config", (request, response) -> {
            response.header("Access-Control-Allow-Methods", "PUT");
            return "";
        });
        put("/api/settings/config", this::putConfigs);
        post("/api/settings/changeAccount", this::changeAccount);
        get("/api/settings/reload", (request, response) -> {
            client.reload();
            return "true";
        });

        post("/api/suggestion", this::getFileSuggestion);

        post("/api/send", this::send);

        get("/resources/background", this::resourcesBackground);

        get("/api/status", this::getStatus);
        get("/api/folders", this::getFolders);
        get("/api/folders/:folder/messages", this::getMessageCollections);
        get("/api/messages/:msgid", this::getMessage);
        get("/api/messages/:msgid/cid/:cid", this::getCidContent);
        get("/api/messages/:msgid/att/:att", this::openAttachment);

        options("/api/messages/:msgid", (request, response) -> {
            response.header("Access-Control-Allow-Methods", "PATCH");
            return "";
        });

        patch("/api/messages/:msgid", (request, response) -> {
            return client.getStore().doMessageQuery(request.params("msgid"), msg -> {
                String unread = request.queryParams("unread");
                if (unread != null) {
                    boolean unreadBool = unread.equals("true");
                    msg.setFlag(Flag.SEEN, !unreadBool);
                }

                String flagged = request.queryParams("flagged");
                if (flagged != null) {
                    boolean flaggedBool = flagged.equals("true");
                    msg.setFlag(Flag.FLAGGED, flaggedBool);
                }
                return "true";
            });
        });

        after((request, response) -> {
            if (response.type() == null && response.status() == 200) {
                response.type("application/json");
            }
            response.header("Access-Control-Allow-Origin", "*");
        });

        Spark.exception(Exception.class, (exception, request, response) -> {
            exception.printStackTrace();
        });
    }

    private Object putConfigs(Request req, Response res) throws Exception {
        JsonElement element = new JsonParser().parse(req.body());
        if (!element.isJsonObject()) {
            res.status(400);
            return "Invalid request format";
        }
        JsonObject object = element.getAsJsonObject();
        for (Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().getAsString();
            Client.getInstance().putConfiguration(key, value);
            log.info("config {} updated", key);
        }
        return "true";
    }

    private Object changeAccount(Request req, Response res) throws Exception {
        Client.getInstance().changeAccount();
        return "true";
    }

    private Object getStatus(Request req, Response res) throws Exception {
        if (Client.getInstance().getConfiguration("mail.imap.username") == null) {
            return "\"INIT\"";
        } else {
            return "\"NORMAL\"";
        }
    }

    private Object getFileSuggestion(Request req, Response res) throws Exception {
        JsonElement element = new JsonParser().parse(req.body());
        if (!element.isJsonObject()) {
            res.status(400);
            return "Invalid request format";
        }
        JsonObject object = element.getAsJsonObject();
        String data = object.get("data").getAsString();
        Optional<Path> path = client.getFileSuggestion(data);
        if (!path.isPresent()) {
            return "null";
        }
        return new JsonPrimitive(path.get().toString()).toString();
    }

    private Object send(Request req, Response res) throws Exception {
        Properties properties = new Properties();
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.from", client.getConfiguration("mail.address"));
        properties.put("mail.smtp.host", client.getConfiguration("mail.smtp.host"));
        properties.put("mail.smtp.ssl.enable", true);
        properties.put("mail.smtp.auth", true);

        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(client.getConfiguration("mail.smtp.username"),
                        client.getConfiguration("mail.smtp.password"));
            }
        });

        Transport transport = session.getTransport();

        JsonElement element = new JsonParser().parse(req.body());
        JsonObject object = element.getAsJsonObject();

        MimeMessage msg = new MimeMessage(session);
        for (JsonElement e : object.get("to").getAsJsonArray()) {
            JsonObject contact = e.getAsJsonObject();
            InternetAddress addr = new InternetAddress(contact.get("address").getAsString(),
                    contact.get("name").getAsString());
            msg.addRecipient(RecipientType.TO, addr);
        }
        for (JsonElement e : object.get("cc").getAsJsonArray()) {
            JsonObject contact = e.getAsJsonObject();
            InternetAddress addr = new InternetAddress(contact.get("address").getAsString(),
                    contact.get("name").getAsString());
            msg.addRecipient(RecipientType.CC, addr);
        }
        for (JsonElement e : object.get("bcc").getAsJsonArray()) {
            JsonObject contact = e.getAsJsonObject();
            InternetAddress addr = new InternetAddress(contact.get("address").getAsString(),
                    contact.get("name").getAsString());
            msg.addRecipient(RecipientType.BCC, addr);
        }

        String name = client.getConfiguration("mail.name");
        String address = client.getConfiguration("mail.address");

        InternetAddress addr = new InternetAddress(address, name);
        msg.setFrom(addr);

        msg.setSubject(object.get("subject").getAsString());

        List<String> attachment = new ArrayList<>();

        for (JsonElement e : object.get("attachment").getAsJsonArray()) {
            attachment.add(e.getAsString());
        }

        if (attachment.isEmpty()) {
            msg.setContent(object.get("content").getAsString(), "text/html");
        } else {
            MimeMultipart multipart = new MimeMultipart();
            MimeBodyPart bodyPart = new MimeBodyPart();
            bodyPart.setContent(object.get("content").getAsString(), "text/html");
            multipart.addBodyPart(bodyPart);

            for (String att : attachment) {
                File attFile = new File(att);
                MimeBodyPart attPart = new MimeBodyPart();
                DataSource source = new FileDataSource(attFile);
                attPart.setDataHandler(new DataHandler(source));
                attPart.setFileName(attFile.getName());
                multipart.addBodyPart(attPart);
            }

            msg.setContent(multipart);
        }

        String reply = object.get("inReplyTo").getAsString();
        if (!reply.isEmpty()) {
            msg.addHeader("In-Reply-To", reply);
        }

        transport.connect();
        transport.sendMessage(msg, msg.getAllRecipients());

        return "true";
    }

    private Object getMessage(Request req, Response res) throws Exception {
        String msgid = req.params("msgid");

        return client.getStore().doMessageQuery(msgid, (msg) -> {
            JsonObject msgObj = new JsonObject();

            Part body = Messages.getBodyPart(msg, true);

            msgObj.addProperty("content-type", new ContentType(body.getContentType()).getBaseType().toLowerCase());
            msgObj.addProperty("content", (String) body.getContent());

            List<Part> attachments = MailUtil.getAttachments(msg);
            JsonArray arr = new JsonArray();

            int attid = 1;
            for (Part p : attachments) {
                if (StringUtils.isBlank(p.getFileName())) {
                    arr.add("ATT" + attid);
                } else {
                    arr.add(p.getFileName());
                }
                attid++;
            }

            msgObj.add("attachment", arr);

            return msgObj.toString();
        });
    }

    private Object getCidContent(Request req, Response res) throws Exception {
        String msgid = req.params("msgid");
        String cid = req.params("cid");

        return client.getStore().doMessageQuery(msgid, msg -> {
            InputStream in = MailUtil.searchAttachments(msg, cid);

            if (in != null) {
                in = new BufferedInputStream(in);
                res.type(URLConnection.guessContentTypeFromStream(in));
                return IOUtils.readBytes(in);
            }

            return null;
        });
    }

    private Object openAttachment(Request req, Response res) throws Exception {
        String msgid = req.params("msgid");
        String att = req.params("att");

        return client.getStore().doMessageQuery(msgid, msg -> {
            Part attachment;

            if (att.startsWith("ATT")) {
                int attid = Integer.parseInt(att.replace("ATT", ""));
                attachment = MailUtil.searchAttachmentsByIndex(msg, attid, new IntHolder(1));
            } else {
                attachment = MailUtil.searchAttachmentsByName(msg, att);
            }

            if (attachment != null) {
                log.info("Opening attachment " + att);

                String extension = Files.getFileExtension(att);
                if (!extension.isEmpty())
                    extension = "." + extension;

                if (directOpenExtension.contains(extension)) {
                    File file = File.createTempFile("group-charlie", extension);
                    if (Desktop.isDesktopSupported()) {
                        file.deleteOnExit();
                        IOUtils.save(attachment.getInputStream(), file);
                        Desktop.getDesktop().open(file);
                        return "\"SUCCESS\"";
                    }
                }

                File file = new FileChooser().title("Save Attachment").file(att).save();
                if (file != null) {
                    IOUtils.save(attachment.getInputStream(), file);
                    return "\"SUCCESS\"";
                } else {
                    return "\"CANCELLED\"";
                }
            } else {
                res.status(404);
                log.error("Cannot find attachment " + att);
                return "cannot open the attachment " + att;
            }
        });
    }

    public static void main(String[] args) {
        WebUIServer.getInstance();

    }

    private String parentFolder(String name) {
        int id = name.lastIndexOf('/');
        if (id == -1)
            return null;
        return name.substring(0, id);
    }

    private String folderName(String name) {
        int id = name.lastIndexOf('/');
        if (id == -1)
            return name;
        return name.substring(id + 1);
    }

    private Object getFolders(Request req, Response res) {
        JsonArray foldersJSON = new JsonArray();

        LinkedHashMap<String, JsonObject> objs = client.getStore().doQuery(store -> {
            LinkedHashMap<String, JsonObject> ret = new LinkedHashMap<>();
            for (Entry<String, Folder> e : client.getStore().getFolders(store).entrySet()) {
                try {
                    JsonObject folder = new JsonObject();
                    folder.addProperty("name", e.getKey());
                    folder.addProperty("messages", e.getValue().getMessageCount());
                    folder.addProperty("unread", e.getValue().getUnreadMessageCount());
                    JsonArray subfolder = new JsonArray();
                    folder.add("subfolder", subfolder);
                    ret.put(e.getKey(), folder);
                } catch (MessagingException ex) {
                    ex.printStackTrace();
                }
            }
            return ret;
        });

        objs.entrySet().stream().forEach(e -> {
            String name = e.getKey();
            String parent = parentFolder(name);
            if (parent == null) {
                foldersJSON.add(e.getValue());
            } else {
                JsonObject parentObj = objs.get(parent);
                if (parentObj == null) {
                    // Unlikely, but we treat this as a special case
                    foldersJSON.add(e.getValue());
                } else {
                    // If we get a parent folder, then remove the path part
                    JsonObject obj = e.getValue();
                    obj.addProperty("name", folderName(obj.get("name").getAsString()));
                    parentObj.get("subfolder").getAsJsonArray().add(obj);
                }
            }
        });

        return foldersJSON.toString();
    }

    private String getMessageCollections(Request req, Response res) throws Exception {
        String folder = req.params("folder");
        String start = req.queryParams("start");
        String end = req.queryParams("end");
        if (start == null || end == null) {
            res.status(400);
            return "start or end cannot be empty";
        }

        int startNum;
        int endNum;
        try {
            startNum = Integer.valueOf(start);
            endNum = Integer.valueOf(end);
        } catch (NumberFormatException e) {
            res.status(400);
            return "start or end need to be numbers";
        }

        return client.getStore().doFolderQuery(folder, f -> {
            if (f == null) {
                res.status(404);
                return "folder does not exist";
            }

            if (!f.isOpen())
                f.open(Folder.READ_WRITE);

            JsonArray messages = new JsonArray();

            Message[] msgs = f.getMessages(startNum, endNum);

            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.FLAGS);
            fp.add(FetchProfile.Item.CONTENT_INFO);

            f.fetch(msgs, fp);

            for (int i = 0; i < msgs.length; i++) {
                JsonObject msgObj = new JsonObject();
                InternetAddress sender = (InternetAddress) msgs[i].getFrom()[0];
                msgObj.add("from", serializeContact(sender));

                JsonArray to = serializeContactCollection(msgs[i].getRecipients(Message.RecipientType.TO));

                JsonArray cc = serializeContactCollection(msgs[i].getRecipients(Message.RecipientType.CC));

                JsonArray bcc = serializeContactCollection(msgs[i].getRecipients(Message.RecipientType.BCC));

                msgObj.add("to", to);
                msgObj.add("cc", cc);
                msgObj.add("bcc", bcc);

                msgObj.addProperty("subject", msgs[i].getSubject());
                msgObj.addProperty("date", msgs[i].getSentDate().getTime());
                Object content = Messages.fastGetContent(msgs[i]);
                if (content != null) {
                    String summary = Messages.getBodyText(msgs[i]).trim();
                    summary = summary.substring(0, Math.min(summary.length(), 100));
                    msgObj.addProperty("summary", summary);
                } else {
                    msgObj.addProperty("summary", "");
                }
                msgObj.addProperty("unread", !msgs[i].getFlags().contains(Flag.SEEN));
                msgObj.addProperty("flagged", msgs[i].getFlags().contains(Flag.FLAGGED));
                msgObj.addProperty("inReplyTo", Messages.getInReplyTo(msgs[i]));
                msgObj.addProperty("mid", Messages.getMessageID(msgs[i]));

                msgObj.addProperty("hasAttachment", msgs[i].isMimeType("multipart/mixed"));

                messages.add(msgObj);
            }

            return messages.toString();
        });

    }
}
