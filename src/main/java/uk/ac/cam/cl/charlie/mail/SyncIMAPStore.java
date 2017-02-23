package uk.ac.cam.cl.charlie.mail;

import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import org.iq80.leveldb.DBIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.imap.IMAPStore;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.db.Serializers;
import uk.ac.cam.cl.charlie.util.PeekableIterator;

public class SyncIMAPStore extends Store {

    static class SerializedFolder {
        long uidvalidity;
        int types;
        Date lastsync;
    }

    private static Serializer<SerializedFolder> FOLDER_SERIALIZER = new Serializer<SerializedFolder>() {

        @Override
        public boolean typecheck(Object obj) {
            return obj instanceof SerializedFolder;
        }

        @Override
        public byte[] serialize(SerializedFolder object) {
            ByteBuffer bb = ByteBuffer.allocate(20);
            bb.putLong(object.uidvalidity);
            bb.putInt(object.types);
            bb.putLong(object.lastsync.getTime());
            return bb.array();
        }

        @Override
        public SerializedFolder deserialize(byte[] bytes) {
            if (bytes.length != 20) {
                throw new RuntimeException("Length should be 12");
            }
            ByteBuffer bb = ByteBuffer.wrap(bytes);

            SerializedFolder ret = new SerializedFolder();
            ret.uidvalidity = bb.getLong();
            ret.types = bb.getInt();
            ret.lastsync = new Date(bb.getLong());
            return ret;
        }

    };

    private static Logger log = LoggerFactory.getLogger(SyncIMAPStore.class);

    IMAPStore store = new IMAPStore(session, url);
    PersistentMap<String, SerializedFolder> database;

    // Contains all open folders
    // Use WeakReference to avoid leaking memory
    HashMap<String, SoftReference<SyncIMAPFolder>> folders = new HashMap<>();

    SyncIMAPFolder defaultFolder;

    SyncIMAPFolder inbox;

    String user;
    String password;

    public SyncIMAPStore(Session session, URLName urlname) {
        super(session, urlname);

        database = Database.getInstance().getMap("folders", Serializers.STRING, FOLDER_SERIALIZER);

        // Construct the root folder
        SerializedFolder sf = new SerializedFolder();
        sf.uidvalidity = 0;
        sf.types = Folder.HOLDS_FOLDERS;
        sf.lastsync = new Date(0);
        defaultFolder = new SyncIMAPFolder(this, "");
        defaultFolder.updateFromSerialized(sf);
    }

    protected void connectRemote() throws MessagingException {
        if (store != null) {
            store = (IMAPStore) session.getStore("imap");
        }

        if (!store.isConnected()) {
            store.connect(user, password);
        }
    }

    private void synchronize() {
        try {
            connectRemote();

            // List all folders
            Folder[] folders = store.getDefaultFolder().list("*");
            // Use tree map here since we want the folders to be sorted
            // for comparision
            TreeMap<String, Folder> map = new TreeMap<>();

            for (Folder f : folders) {
                map.put(f.getFullName(), f);
            }

            DBIterator dbiter = database.getLevelDB().iterator();
            PeekableIterator<Entry<String, Folder>> iter = new PeekableIterator<>(map.entrySet().iterator());

            // Check difference betweent server folders and local folders
            while (dbiter.hasNext()) {
                Entry<byte[], byte[]> entry = dbiter.next();
                String key = Serializers.STRING.deserialize(entry.getKey());

                while (true) {
                    if (!iter.hasNext()) {
                        // Finished all entries, so this is extra
                        log.info("Folder {} disappears from server", key);
                        database.remove(key);
                        break;
                    } else {
                        int result = key.compareTo(iter.peek().getKey());
                        if (result == 0) {
                            // Same key, that's what we like
                            iter.next();
                            break;
                        } else if (result < 0) {
                            log.info("Folder {} disappears from server", key);
                            database.remove(key);
                            break;
                        } else {
                            log.info("Folder {} appeared on the server", key);
                            SerializedFolder folder = new SerializedFolder();
                            folder.uidvalidity = 0;
                            folder.types = iter.next().getValue().getType();
                            folder.lastsync = new Date(0);
                            database.put(key, folder);
                            continue;
                        }
                    }
                }
            }

            while (iter.hasNext()) {
                Entry<String, Folder> entry = iter.next();
                String key = entry.getKey();

                log.info("Folder {} appeared on the server", key);
                SerializedFolder folder = new SerializedFolder();
                folder.uidvalidity = 0;
                folder.types = entry.getValue().getType();
                folder.lastsync = new Date(0);
                database.put(key, folder);
            }

            updateFolders();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    private void updateFolders() {
        for (Entry<String, SoftReference<SyncIMAPFolder>> e : folders.entrySet()) {
            SyncIMAPFolder folder = e.getValue().get();
            folder.updateFromSerialized(database.get(folder));
        }
    }

    @Override
    protected boolean protocolConnect(String host, int port, String user, String password) throws MessagingException {
        this.user = user;
        this.password = password;

        if (database.isEmpty()) {
            // First time using connect remote
            connectRemote();

            synchronize();
        } else {
            connectRemote();

            synchronize();
        }
        return true;
    }

    @Override
    public Folder getDefaultFolder() throws MessagingException {
        return defaultFolder;
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        if (name.isEmpty()) {
            return getDefaultFolder();
        }

        // If there's a live reference, then take it
        SoftReference<SyncIMAPFolder> weakfolder = folders.get(name);
        SyncIMAPFolder folder = weakfolder == null ? null : weakfolder.get();

        if (folder != null)
            return folder;

        SerializedFolder arr = database.get(name);
        if (arr == null) {
            // A NX folder
            folder = new SyncIMAPFolder(this, name);
            folder.updateFromSerialized(null);
        } else {
            folder = new SyncIMAPFolder(this, name);
            folder.updateFromSerialized(arr);
        }

        // TODO: Add cleanup code for SoftReference
        folders.put(name, new SoftReference<>(folder));

        return folder;
    }

    @Override
    public Folder getFolder(URLName url) throws MessagingException {
        throw new UnsupportedOperationException("getFolder(URLName) is not supported");
    }

}
