package uk.ac.cam.cl.charlie.ui;

import static spark.Spark.after;
import static spark.Spark.get;
import static spark.Spark.ipAddress;
import static spark.Spark.options;
import static spark.Spark.patch;
import static spark.Spark.port;
import static spark.Spark.staticFiles;

import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.ContentType;
import javax.mail.internet.InternetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
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

        get("/api/settings/config/:key", (request, response) -> {
            String key = request.params("key");
            try {
                String value = Client.getInstance().getConfiguration(key);
                if (value == null)
                    return "null";
                return new JsonPrimitive(value).toString();
            } catch (RuntimeException e) {
                response.status(404);
                return e.getMessage();
            }
        });

        get("/resources/background", this::resourcesBackground);

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

                msgObj.addProperty("hasAttachment", msgs[i].isMimeType("multipart/mixed"));

                messages.add(msgObj);
            }

            return messages.toString();
        });

    }
}