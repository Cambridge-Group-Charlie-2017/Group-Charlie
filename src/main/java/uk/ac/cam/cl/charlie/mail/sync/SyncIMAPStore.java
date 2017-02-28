package uk.ac.cam.cl.charlie.mail.sync;

import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import javax.mail.AuthenticationFailedException;
import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.MapMaker;
import com.sun.mail.imap.IMAPStore;

import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.db.Serializers;
import uk.ac.cam.cl.charlie.util.Deferred;

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

    static interface SyncTask {
        public abstract void perform(IMAPStore store) throws MessagingException;
    }

    static class Task {
        SyncTask task;
        Deferred<Boolean> deferred;
        boolean canRetry;

        Task(SyncTask task, Deferred<Boolean> deferred, boolean canRetry) {
            this.task = task;
            this.deferred = deferred;
            this.canRetry = canRetry;
        }
    }

    // In this implementation all IMAP handling will be done in the same
    // thread
    // to prevent synchronization problems. This is the executor, and all
    // tasks
    // will need to be submitted to this
    private Thread executor;
    private LinkedBlockingDeque<Task> synctasks = new LinkedBlockingDeque<>();

    // All components in this package is quite tightly related so share this
    // with the package
    static Logger log = LoggerFactory.getLogger(SyncIMAPStore.class.getPackage().getName());

    private IMAPStore store = new IMAPStore(session, url);
    PersistentMap<String, SerializedFolder> database;

    // Contains all open folders
    // Use WeakReference to avoid leaking memory
    // XXX: We could use a strong referenced map here as keeping all folders
    // in
    // memory is not expensive and we need these objects during
    // synchronization.
    // However, we do want the non-existence folder to be claimed
    // automatically.
    // Maybe two seperate maps?
    ConcurrentMap<String, SyncIMAPFolder> folders = new MapMaker().weakValues().makeMap();

    SyncIMAPFolder defaultFolder;

    private String user;
    private String password;

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

    private void connectRemote() throws MessagingException {
        if (store != null) {
            store = (IMAPStore) session.getStore("imap");
        }

        if (!store.isConnected()) {
            store.connect(user, password);
        }
    }

    /*
     * Do a full synchronization of all folders and contained messages
     */
    private void fullSynchronize(IMAPStore store) throws MessagingException {
        // List all folders
        Folder[] folders = store.getDefaultFolder().list("*");
        // Use tree map here since we want the folders to be sorted
        // for comparision
        TreeMap<String, Folder> map = new TreeMap<>();

        for (Folder f : folders) {
            map.put(f.getFullName(), f);
        }

        new SortedDiff<String, SerializedFolder, Folder>() {

            @Override
            protected void onRemove(Entry<String, SerializedFolder> entry) {
                database.remove(entry.getKey());
            }

            @Override
            protected void onAdd(Entry<String, Folder> entry) {
                try {
                    SerializedFolder folder = new SerializedFolder();
                    folder.uidvalidity = 0;
                    folder.types = entry.getValue().getType();
                    folder.lastsync = new Date(0);
                    database.put(entry.getKey(), folder);
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
            }

        }.diff(database.entrySet().iterator(), map.entrySet().iterator());

        updateFolders();

        for (Entry<String, SerializedFolder> e : database.entrySet()) {
            ((SyncIMAPFolder) getFolder(e.getKey())).doSynchronize(store);
        }
    }

    private void updateFolders() {
        for (Entry<String, SyncIMAPFolder> e : folders.entrySet()) {
            SyncIMAPFolder folder = e.getValue();
            folder.updateFromSerialized(database.get(e.getKey()));
        }
    }

    @Override
    protected boolean protocolConnect(String host, int port, String user, String password) throws MessagingException {
        this.user = user;
        this.password = password;

        if (database.isEmpty()) {
            // First time this must succeed
            // So we run it on the main thread
            connectRemote();
            fullSynchronize(store);
        }

        executor = new Thread(this::run);
        executor.start();
        return true;
    }

    @Override
    public void close() {
        if (executor != null) {
            setConnected(false);
            // Stop executor from polling tasks
            executor.interrupt();
            try {
                executor.join();
            } catch (InterruptedException e) {
                // No thread should be interrupting
                throw new AssertionError(e);
            }
            executor = null;
        }
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

        SyncIMAPFolder folder = folders.get(name);

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

        folders.put(name, folder);

        return folder;
    }

    @Override
    public Folder getFolder(URLName url) throws MessagingException {
        throw new UnsupportedOperationException("getFolder(URLName) is not supported");
    }

    protected Future<Boolean> submitUpdate(SyncTask task) {
        Deferred<Boolean> deferred = new Deferred<>();
        synctasks.add(new Task(task, deferred, true));
        return deferred;
    }

    protected Future<Boolean> submitQuery(SyncTask task) {
        Deferred<Boolean> deferred = new Deferred<>();
        synctasks.add(new Task(task, deferred, false));
        return deferred;
    }

    private void run() {
        // When we start, do a initial full synchronization
        try {
            connectRemote();
            fullSynchronize(store);
        } catch (MessagingException e) {
            if (!store.isConnected() && !(e instanceof AuthenticationFailedException)) {
                // Cannot connect to mail server
                e.printStackTrace();
                log.info("Failed to connect to mail server");
            } else {
                e.printStackTrace();
            }
        }

        while (isConnected()) {
            try {
                // If we don't have any tasks in 300 seconds, run a full
                // synchronization. Technically we can use IMAP's IDLE
                // functionality, but JavaMail states that the
                // implementation is
                // experimental
                Task task = synctasks.poll(300, TimeUnit.SECONDS);
                if (task == null) {
                    try {
                        connectRemote();
                        fullSynchronize(store);
                    } catch (MessagingException e) {
                        if (!store.isConnected() && !(e instanceof AuthenticationFailedException)) {
                            // Cannot connect to mail server
                            log.info("Failed to connect to mail server");
                        } else {
                            e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        connectRemote();
                        task.task.perform(store);
                        task.deferred.setValue(true);
                    } catch (MessagingException e) {
                        if (!store.isConnected() && !(e instanceof AuthenticationFailedException)) {
                            // Add a placeholder
                            synctasks.add(task);
                            Task processing = synctasks.poll();

                            // Reject all pending queries
                            while (processing != task) {
                                if (processing.canRetry) {
                                    synctasks.add(processing);
                                } else {
                                    processing.deferred.setValue(false);
                                }
                                processing = synctasks.poll();
                            }

                            if (task.canRetry) {
                                synctasks.addFirst(task);
                                e.printStackTrace();
                                log.info("Failed to connect to mail server");
                                Thread.sleep(60000);
                            } else {
                                processing.deferred.setValue(false);
                            }
                        } else {
                            task.deferred.throwException(e);
                        }
                    }
                }
            } catch (InterruptedException e) {
                // Interrupted, we might be closing the connection
                continue;
            }
        }
    }

    protected Session getSession() {
        return session;
    }

}
