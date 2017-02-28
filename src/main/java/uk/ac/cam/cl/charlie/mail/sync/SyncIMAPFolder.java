package uk.ac.cam.cl.charlie.mail.sync;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.mail.Address;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPMessage;
import com.sun.mail.imap.IMAPStore;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializers;
import uk.ac.cam.cl.charlie.mail.sync.SyncIMAPStore.SerializedFolder;
import uk.ac.cam.cl.charlie.util.ObjectHolder;

public class SyncIMAPFolder extends Folder implements UIDFolder {

    /*
     * Get the status of the message. 1 indicates only envelope is fetched, 2
     * indicates all contents are fetched.
     */
    int deserializeStatus(byte[] array) {
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

    Flags deserializeFlags(byte[] array) {
        ByteArrayInputStream bis = new ByteArrayInputStream(array);
        DataInputStream dis = new DataInputStream(bis);
        try {
            dis.skipBytes(4);

            return deserializeFlags(dis);
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static Logger log = LoggerFactory.getLogger(SyncIMAPFolder.class.getPackage().getName());
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

    // TODO: Make this persistent
    LinkedList<Change> offlineChanges = new LinkedList<>();

    public SyncIMAPFolder(SyncIMAPStore store, String fullname) {
        super(store);

        this.fullname = fullname;

        messages = new MapMaker().weakValues().makeMap();
    }

    private void teardownAll() {
        throw new Error("Stupid server change UID");
    }

    /*
     * Rebuild the ID->UID map
     */
    private void rebuildUid() {
        idUidMap.clear();

        for (long uid : map.keySet()) {
            lastseenuid = uid;
            idUidMap.add(uid);
        }

        msgcount = idUidMap.size();
    }

    /*
     * Download a message from remote server
     */
    void downloadMessage(long uid) throws MessagingException {
        // Ensure connection
        SyncIMAPStore store = (SyncIMAPStore) this.store;
        Future<Boolean> f = store.submitQuery((imapStore) -> {
            IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullname);

            long validity = imapFolder.getUIDValidity();
            if (validity != this.validity) {
                // UID does not make sense now
                return;
            }

            log.info("{}/{}: Downloading message", fullname, uid);

            imapFolder.open(READ_ONLY);
            try {
                byte[] bytes = serializeWithContent((IMAPMessage) imapFolder.getMessageByUID(uid));
                map.put(uid, bytes);
            } finally {
                imapFolder.close(false);
            }
        });
        try {
            if (f.get()) {
                return;
            } else {
                throw new MessagingException("Cannot load message from the server");
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /*
     * Upload local changes to server
     */
    protected void synchronizeUpdate(IMAPFolder imapFolder) throws MessagingException {
        imapFolder.open(IMAPFolder.READ_WRITE);

        try {
            while (!offlineChanges.isEmpty()) {
                Change change = offlineChanges.removeFirst();
                change.perform(imapFolder);
            }
        } finally {
            imapFolder.close(false);
        }
    }

    /*
     * Synchronize local copy with the server copy
     */
    protected void doSynchronize(IMAPStore imapStore) throws MessagingException {
        log.info("{}: Start synchronization", fullname);

        // Ensure connection
        SyncIMAPStore store = (SyncIMAPStore) this.store;

        IMAPFolder imapFolder = (IMAPFolder) imapStore.getFolder(fullname);

        // Only synchronize if the folder can contain messages
        if ((imapFolder.getType() & Folder.HOLDS_MESSAGES) != 0) {
            long validity = imapFolder.getUIDValidity();
            if (validity != this.validity) {
                if (this.validity != 0) {
                    log.info("{}: UID invalidated", fullname);
                    teardownAll();
                }
                this.validity = validity;
            }

            ObjectHolder<Boolean> dirty = new ObjectHolder<>(false);

            log.info("{}: Last seen UID is {}", fullname, lastseenuid);

            synchronizeUpdate(imapFolder);

            imapFolder.open(Folder.READ_ONLY);

            try {
                Message[] message = imapFolder.getMessagesByUID(lastseenuid + 1, IMAPFolder.LASTUID);

                if (message.length != 0 && imapFolder.getUID(message[message.length - 1]) > lastseenuid) {
                    dirty.value = true;

                    FetchProfile profile = new FetchProfile();
                    profile.add(FetchProfile.Item.ENVELOPE);
                    profile.add(FetchProfile.Item.CONTENT_INFO);
                    profile.add(FetchProfile.Item.FLAGS);

                    log.info("{}: Found {} new emails", fullname, message.length);

                    for (int i = 0; i < message.length; i += 1024) {
                        int len = i + 1024 < message.length ? 1024 : message.length - i;
                        Message[] chunk = new Message[len];
                        System.arraycopy(message, i, chunk, 0, len);
                        // Clean up to allow GC
                        Arrays.fill(message, i, i + len, null);

                        log.info("{}: Fetching email {} to {}", fullname, imapFolder.getUID(chunk[0]),
                                imapFolder.getUID(chunk[len - 1]));

                        imapFolder.fetch(chunk, profile);

                        for (Message m : chunk) {
                            IMAPMessage imapMessage = (IMAPMessage) m;
                            long uid = imapFolder.getUID(m);
                            byte[] serialized;
                            // if (m.getSize() > 16384) {
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

                // Now checking flags of all fetched messages
                Message[] oldMessage = imapFolder.getMessagesByUID(1, lastseenuid);
                if (oldMessage.length != 0) {
                    log.info("{}: Checking update for {} old emails", fullname, oldMessage.length);

                    FetchProfile profile = new FetchProfile();
                    profile.add(FetchProfile.Item.FLAGS);
                    imapFolder.fetch(oldMessage, profile);

                    new SortedDiff<Long, byte[], Message>() {
                        @Override
                        protected void onRemove(Entry<Long, byte[]> entry) {
                            long key = entry.getKey();
                            if (key > lastseenuid) {
                                // It's newly fetched emails instead of emails
                                // that should be removed!
                                breakExecution();
                            }

                            messages.remove(key);
                            map.remove(key);

                            log.info("{}/{}: Email removed", fullname, key);

                            dirty.value = true;
                        }

                        @Override
                        protected void onNoChange(Entry<Long, byte[]> e1, Entry<Long, Message> e2) {
                            long uid = e1.getKey();
                            try {
                                Flags newFlags = e2.getValue().getFlags();

                                // Use existing message object if it exists
                                SyncIMAPMessage localmsg = messages.get(uid);

                                if (localmsg == null) {
                                    // Fetch only flags if message is not in
                                    // memory
                                    // avoiding do the full deserialization
                                    Flags oldFlags = deserializeFlags(map.get(uid));
                                    if (!newFlags.equals(oldFlags)) {
                                        // If the flag is changed then we do the
                                        // entire deserialization. In theory
                                        // this
                                        // can be avoided but it's
                                        // quite complex
                                        localmsg = getMessageByUID(uid);
                                    }
                                } else {
                                    // If we don't need to change anything then
                                    // set
                                    // localmsg to null
                                    if (localmsg.getFlags().equals(newFlags)) {
                                        localmsg = null;
                                    }
                                }

                                if (localmsg != null) {
                                    // Override flags stored locally
                                    log.info("{}/{}: Flag changed", fullname, uid);
                                    localmsg.overrideFlags(newFlags);
                                }
                            } catch (MessagingException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        protected void onAdd(Entry<Long, Message> entry) {
                            // This should be impossible
                            log.error("Unexpected message appeared {}", entry.getKey());
                        }

                    }.diff(map.entrySet().iterator(), Arrays.stream(oldMessage).<Entry<Long, Message>>map(m -> {
                        try {
                            return new AbstractMap.SimpleEntry<>(imapFolder.getUID(m), m);
                        } catch (MessagingException e) {
                            throw new RuntimeException(e);
                        }
                    }).iterator());

                    imapFolder.fetch(oldMessage, profile);
                }

                if (dirty.value) {
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

    public void synchronize() {
        ((SyncIMAPStore) store).submitUpdate(this::doSynchronize);
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

            ArrayList<Folder> list = new ArrayList<>();

            for (String key : store.database.keySet()) {
                list.add(store.getFolder(key));
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
        if (!exists) {
            throw new MessagingException("Folder does not exist");
        }
        return msgcount;
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        if (!exists) {
            throw new MessagingException("Folder does not exist");
        }

        if (msgnum <= 0 || msgnum > msgcount) {
            throw new MessagingException("Message id out of bound");
        }

        long uid = idUidMap.get(msgnum - 1);
        return getMessageByUID(uid);
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        throw new UnsupportedOperationException("appendMessages not supported");
    }

    @Override
    public Message[] expunge() throws MessagingException {
        throw new UnsupportedOperationException("expunge not supported");
    }

    /*
     * Called by SyncIMAPStore when the folder structure is changed
     */
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

    protected void enqueueChange(Change change) {
        offlineChanges.add(change);
    }

    @Override
    public void finalize() {
        log.info("{}: finalized", fullname);
    }

    @Override
    public long getUIDValidity() throws MessagingException {
        return validity;
    }

    @Override
    public SyncIMAPMessage getMessageByUID(long uid) throws MessagingException {
        if (!exists) {
            throw new MessagingException("Folder does not exist");
        }

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
            msg.initialized = true;
            messages.put(uid, msg);
        }

        return msg;
    }

    @Override
    public Message[] getMessagesByUID(long start, long end) throws MessagingException {
        throw new UnsupportedOperationException("getMessagesByUID(long, long) not implemented");
    }

    @Override
    public Message[] getMessagesByUID(long[] uids) throws MessagingException {
        throw new UnsupportedOperationException("getMessagesByUID(long[]) not implemented");
    }

    @Override
    public long getUID(Message message) throws MessagingException {
        if (message instanceof SyncIMAPMessage) {
            return ((SyncIMAPMessage) message).uid;
        }
        throw new IllegalArgumentException("Unexpected message");
    }

    protected void flushChange(SyncIMAPMessage message) throws MessagingException {
        if (deserializeStatus(map.get(message.uid)) == 1) {
            map.put(message.uid, serializeWithoutContent(message));
        } else {
            map.put(message.uid, serializeWithContent(message));
        }
    }

    @Override
    public SyncIMAPStore getStore() {
        return (SyncIMAPStore) store;
    }

}
