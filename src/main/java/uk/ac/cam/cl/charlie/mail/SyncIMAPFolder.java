package uk.ac.cam.cl.charlie.mail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.iq80.leveldb.DBIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializers;
import uk.ac.cam.cl.charlie.mail.SyncIMAPStore.SerializedFolder;

public class SyncIMAPFolder extends Folder {

    /*
     * Get the status of the message. 1 indicates only envelope is fetched, 2
     * indicates all contents are fetched.
     */
    private int deserializeStatus(byte[] array) {
        return ByteBuffer.wrap(array).getInt();
    }

    /*
     * Serialize addresses from stream.
     */
    private void serializeInternetAddresses(DataOutputStream os, Address... addresses) throws IOException {
        if (addresses == null) {
            os.writeInt(0);
            return;
        }
        int len = 0;
        for (Address addr : addresses) {
            if (addr instanceof InternetAddress)
                len++;
        }
        os.writeInt(len);
        for (Address addr : addresses) {
            if (addr instanceof InternetAddress) {
                String personal = ((InternetAddress) addr).getPersonal();
                String address = ((InternetAddress) addr).getAddress();
                os.writeUTF(personal == null ? "" : personal);
                os.writeUTF(address);
            }
        }
    }

    /*
     * Deserialize addresses to stream.
     */

    private InternetAddress[] deserializeInternetAddresses(DataInputStream is) throws IOException {
        int len = is.readInt();
        if (len == 0)
            return null;

        InternetAddress[] ret = new InternetAddress[len];

        for (int i = 0; i < len; i++) {
            String personal = is.readUTF();
            String address = is.readUTF();
            if (personal.isEmpty())
                personal = null;
            ret[i] = new InternetAddress(address, personal);
        }

        return ret;
    }

    /*
     * Serialize flags of the message. Only DRAFT, SEEN, FLAGGED are considered.
     */
    private void serializeFlags(DataOutputStream os, Flags flags) throws IOException {
        boolean draft = flags.contains(Flag.DRAFT);
        boolean seen = flags.contains(Flag.SEEN);
        boolean flagged = flags.contains(Flag.FLAGGED);
        int len = (draft ? 1 : 0) + (seen ? 1 : 0) + (flagged ? 1 : 0);
        os.writeInt(len);
        if (draft)
            os.writeUTF("DRAFT");
        if (seen)
            os.writeUTF("SEEN");
        if (flagged)
            os.writeUTF("FLAGGED");
    }

    /*
     * Deserialize flags. Only DRAFT, SEEN, FLAGGED are considered.
     */
    private Flags deserializeFlags(DataInputStream is) throws IOException {
        Flags flags = new Flags();

        int len = is.readInt();
        for (int i = 0; i < len; i++) {
            String flag = is.readUTF();
            switch (flag) {
            case "DRAFT":
                flags.add(Flag.DRAFT);
                break;
            case "SEEN":
                flags.add(Flag.SEEN);
                break;
            case "FLAGGED":
                flags.add(Flag.FLAGGED);
                break;
            }
        }
        return flags;
    }

    /*
     * Serialize message to bytes without containing its contents. Require
     * ENVELOPE, CONTENT_INFO, FLAGS to be prefetched.
     */
    private byte[] serializeWithoutContent(MimeMessage m) throws MessagingException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeInt(1);

            serializeFlags(dos, m.getFlags());

            serializeInternetAddresses(dos, m.getFrom());
            serializeInternetAddresses(dos, m.getSender());
            serializeInternetAddresses(dos, m.getReplyTo());
            serializeInternetAddresses(dos, m.getRecipients(RecipientType.TO));
            serializeInternetAddresses(dos, m.getRecipients(RecipientType.CC));
            serializeInternetAddresses(dos, m.getRecipients(RecipientType.BCC));
            dos.writeUTF(m.getSubject() == null ? "" : m.getSubject());

            String inReplyTo;
            if (m instanceof IMAPMessage) {
                inReplyTo = ((IMAPMessage) m).getInReplyTo();
            } else {
                inReplyTo = m.getHeader("In-Reply-To", null);
            }
            if (inReplyTo == null)
                inReplyTo = "";
            dos.writeUTF(inReplyTo);

            dos.writeUTF(m.getMessageID() == null ? "" : m.getMessageID());

            dos.writeLong(m.getSentDate().getTime());

            dos.writeUTF(m.getContentType());
            dos.writeInt(m.getSize());
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return bos.toByteArray();
    }

    /*
     * Deserialize message without content
     */
    private SyncIMAPMessage deserializeWithoutContent(byte[] array, long uid) throws MessagingException {
        ByteArrayInputStream bis = new ByteArrayInputStream(array);
        DataInputStream dis = new DataInputStream(bis);
        SyncIMAPMessage msg = new SyncIMAPMessage(this, uid);

        try {
            if (dis.readInt() != 1) {
                throw new AssertionError("Unexpected tag");
            }

            msg.setFlags(deserializeFlags(dis), true);

            InternetAddress[] from = deserializeInternetAddresses(dis);
            if (from != null)
                msg.setFrom(from[0]);

            InternetAddress[] sender = deserializeInternetAddresses(dis);
            if (sender != null)
                msg.setSender(sender[0]);

            msg.setReplyTo(deserializeInternetAddresses(dis));
            msg.setRecipients(RecipientType.TO, deserializeInternetAddresses(dis));
            msg.setRecipients(RecipientType.CC, deserializeInternetAddresses(dis));
            msg.setRecipients(RecipientType.BCC, deserializeInternetAddresses(dis));

            String subject = dis.readUTF();
            msg.setSubject(subject);

            String inReplyTo = dis.readUTF();
            if (!inReplyTo.isEmpty()) {
                msg.setHeader("In-Reply-To", inReplyTo);
            }

            String msgid = dis.readUTF();
            if (!msgid.isEmpty()) {
                msg.setHeader("Message-ID", msgid);
            }

            msg.setSentDate(new Date(dis.readLong()));

            String contentType = dis.readUTF();
            msg.setHeader("Content-Type", contentType);
        } catch (IOException e) {
            throw new AssertionError(e);
        }

        return msg;
    }

    /*
     * Serialize the entire message including the contents
     */
    private byte[] serializeWithContent(MimeMessage m) throws MessagingException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        try {
            dos.writeInt(2);

            serializeFlags(dos, m.getFlags());

            m.writeTo(dos);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
        return bos.toByteArray();
    }

    /*
     * Deserialize the entire message including the contents
     */
    void deserializeWithContent(byte[] array, SyncIMAPMessage target) throws MessagingException {
        ByteArrayInputStream bis = new ByteArrayInputStream(array);
        DataInputStream dis = new DataInputStream(bis);
        try {
            if (dis.readInt() != 2) {
                throw new RuntimeException("Unexpected tag");
            }

            Flags flags = deserializeFlags(dis);
            target.setFlags(flags, true);
            target.parse(dis);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Logger log = LoggerFactory.getLogger(SyncIMAPFolder.class);
    PersistentMap<Long, byte[]> map;

    String fullname;
    long validity;
    int type;
    boolean exists;
    Date lastsync;

    long lastseenuid;
    int msgcount;
    List<Long> idUidMap = new ArrayList<>();
    ConcurrentMap<Long, SyncIMAPMessage> messages;

    public SyncIMAPFolder(SyncIMAPStore store, String fullname) {
        super(store);

        this.fullname = fullname;

        messages = new MapMaker().weakValues().makeMap();
    }

    public void teardownAll() {
        if (validity == 0) {
            // We are starting synchronization, do no-op
            return;
        }
        throw new Error("Stupid server change UID");
    }

    private void rebuildUid() {
        idUidMap.clear();
        DBIterator iter = map.getLevelDB().iterator();
        iter.seekToFirst();

        while (iter.hasNext()) {
            lastseenuid = Serializers.LONG.deserialize(iter.next().getKey());
            idUidMap.add(lastseenuid);
        }

        msgcount = idUidMap.size();
    }

    void synchronizeMessage(long uid) throws MessagingException {
        // Ensure connection
        SyncIMAPStore store = (SyncIMAPStore) this.store;
        store.connectRemote();
        IMAPStore imapStore = store.store;
        IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullname);

        long validity = imapFolder.getUIDValidity();
        if (validity != this.validity) {
            // UID does not make sense now
            return;
        }

        log.info("Downloading message {}", uid);

        imapFolder.open(READ_ONLY);
        try {
            byte[] bytes = serializeWithContent((IMAPMessage) imapFolder.getMessageByUID(uid));
            map.put(uid, bytes);
        } finally {
            imapFolder.close(false);
        }
    }

    protected void synchronize() throws MessagingException {
        // Ensure connection
        SyncIMAPStore store = (SyncIMAPStore) this.store;
        store.connectRemote();
        IMAPStore imapStore = store.store;
        IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullname);

        // Only synchronize if the folder can contain messages
        if ((imapFolder.getType() & Folder.HOLDS_MESSAGES) != 0) {
            long validity = imapFolder.getUIDValidity();
            if (validity != this.validity) {
                if (this.validity != 0) {
                    log.info("UID is invalid for folder '{}', clearing local copy", fullname);
                    teardownAll();
                }
                this.validity = validity;
            } else {
                log.info("UID is valid for folder '{}'", fullname);
            }

            boolean dirty = false;

            log.info("Last seen UID is {}", lastseenuid);

            imapFolder.open(Folder.READ_ONLY);

            try {
                Message[] message = imapFolder.getMessagesByUID(lastseenuid + 1, IMAPFolder.LASTUID);

                if (message.length != 0 && imapFolder.getUID(message[message.length - 1]) > lastseenuid) {
                    dirty = true;

                    FetchProfile profile = new FetchProfile();
                    profile.add(FetchProfile.Item.ENVELOPE);
                    profile.add(FetchProfile.Item.CONTENT_INFO);
                    profile.add(FetchProfile.Item.FLAGS);

                    log.info("Found {} new emails", message.length);

                    for (int i = 0; i < message.length; i += 1024) {
                        int len = i + 1024 < message.length ? 1024 : message.length - i;
                        Message[] chunk = new Message[len];
                        System.arraycopy(message, i, chunk, 0, len);
                        // Clean up to allow GC
                        Arrays.fill(message, i, i + len, null);

                        log.info("Fetching email {} to {}", imapFolder.getUID(chunk[0]),
                                imapFolder.getUID(chunk[len - 1]));

                        imapFolder.fetch(chunk, profile);

                        for (Message m : chunk) {
                            IMAPMessage imapMessage = (IMAPMessage) m;
                            long uid = imapFolder.getUID(m);
                            byte[] serialized;
                            // if (m.getSize() > 65536) {
                            serialized = serializeWithoutContent(imapMessage);
                            // } else {
                            // log.info("Fetching email {}", uid);
                            // serialized = serializeWithContent(imapMessage);
                            // log.info("Fetched email {}", uid);
                            // }
                            map.put(uid, serialized);
                        }
                    }
                }

                Message[] oldMessage = imapFolder.getMessagesByUID(1, lastseenuid);
                FetchProfile profile = new FetchProfile();
                profile.add(FetchProfile.Item.FLAGS);

                log.info("Checking update for {} old emails", oldMessage.length);

                imapFolder.fetch(oldMessage, profile);

                if (dirty) {
                    rebuildUid();
                }
            } finally {
                imapFolder.close(false);
            }

            // Update lastsync field
            lastsync = new Date();
            SerializedFolder folder = new SerializedFolder();
            folder.uidvalidity = validity;
            folder.types = type;
            folder.lastsync = lastsync;
            store.database.put(fullname, folder);
        }

    }

    private void resync() {
        if (lastsync.getTime() < new Date().getTime() - 60) {
            try {
                synchronize();
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getName() {
        int lastId = fullname.indexOf('/');
        if (lastId == -1)
            return fullname;
        return fullname.substring(lastId + 1);
    }

    @Override
    public String getFullName() {
        return fullname;
    }

    @Override
    public Folder getParent() throws MessagingException {
        int lastId = fullname.indexOf('/');
        if (lastId == -1) {
            if (fullname.isEmpty()) {
                return null;
            }
            return store.getDefaultFolder();
        }
        return store.getFolder(fullname.substring(0, lastId));
    }

    @Override
    public boolean exists() throws MessagingException {
        return exists;
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        if (pattern.equals("*") && fullname.isEmpty()) {
            // call list on root folder
            SyncIMAPStore store = (SyncIMAPStore) this.store;
            DBIterator iter = store.database.getLevelDB().iterator();

            ArrayList<Folder> list = new ArrayList<>();

            while (iter.hasNext()) {
                list.add(store.getFolder(Serializers.STRING.deserialize(iter.next().getKey())));
            }

            return list.toArray(new Folder[list.size()]);
        }
        throw new UnsupportedOperationException("Currently only list * on root is supported");
    }

    @Override
    public char getSeparator() throws MessagingException {
        return '/';
    }

    @Override
    public int getType() throws MessagingException {
        return type;
    }

    @Override
    public boolean create(int type) throws MessagingException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        throw new UnsupportedOperationException("hasNewMessages is not implemented");
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        return store.getFolder(fullname + '/' + name);
    }

    @Override
    public boolean delete(boolean recurse) throws MessagingException {
        throw new UnsupportedOperationException("delete not supported");
    }

    @Override
    public boolean renameTo(Folder f) throws MessagingException {
        throw new UnsupportedOperationException("renameTo not supported");
    }

    @Override
    public void open(int mode) {
        // A local folder is always open
    }

    @Override
    public void close(boolean expunge) {
        // A local folder is always open
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public Flags getPermanentFlags() {
        throw new UnsupportedOperationException("getPermanentFlags not supported");
    }

    @Override
    public int getMessageCount() throws MessagingException {
        resync();
        return msgcount;
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        if (msgnum <= 0 || msgnum > idUidMap.size()) {
            throw new MessagingException("OOB");
        }

        long uid = idUidMap.get(msgnum - 1);
        SyncIMAPMessage msg = messages.get(uid);
        if (msg == null) {
            byte[] bytes = map.get(uid);
            int tag = deserializeStatus(bytes);
            if (tag == 1)
                msg = deserializeWithoutContent(bytes, uid);
            else {
                msg = new SyncIMAPMessage(this, uid);
                deserializeWithContent(bytes, msg);
            }
            messages.put(uid, msg);
        }

        return msg;
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        throw new UnsupportedOperationException("appendMessages not supported");
    }

    @Override
    public Message[] expunge() throws MessagingException {
        throw new UnsupportedOperationException("expunge not supported");
    }

    void updateFromSerialized(SerializedFolder serializedFolder) {
        if (serializedFolder == null) {
            exists = false;
            validity = 0;
            type = Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS;
            lastsync = new Date(0);
        } else {
            if ((serializedFolder.types & Folder.HOLDS_MESSAGES) != 0 && (type & Folder.HOLDS_MESSAGES) == 0
                    && map == null) {
                try {
                    map = Database.getInstance().getMap(
                            "folder-" + Base64.getUrlEncoder().encodeToString(fullname.getBytes("UTF-8")),
                            Serializers.LONG, Serializers.BYTE_ARRAY);

                    rebuildUid();
                } catch (UnsupportedEncodingException e) {
                    throw new AssertionError("UTF-8 should be supported", e);
                }
            }

            exists = true;
            validity = serializedFolder.uidvalidity;
            type = serializedFolder.types;
            lastsync = serializedFolder.lastsync;
        }
    }

}
