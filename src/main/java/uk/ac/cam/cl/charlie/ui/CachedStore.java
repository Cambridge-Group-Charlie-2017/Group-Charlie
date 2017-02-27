package uk.ac.cam.cl.charlie.ui;

import java.util.LinkedHashMap;
import java.util.Properties;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.sun.mail.imap.IMAPMessage;

import uk.ac.cam.cl.charlie.db.Configuration;
import uk.ac.cam.cl.charlie.mail.sync.SyncIMAPStore;

public class CachedStore {

    private static Logger log = LoggerFactory.getLogger(CachedStore.class);

    public interface Query<I, T> {
        T query(I input) throws Exception;
    }

    private Store store;

    /* Access to these variables should be synchronized or used in doQuery */
    LinkedHashMap<String, Folder> folders = new LinkedHashMap<>();
    long foldersLastUpdate = 0;
    private Cache<String, Message> messageCache = CacheBuilder.newBuilder().maximumSize(16).build();

    public CachedStore() {
        store = createStore();
    }

    private Store createStore() {
        try {
            Configuration config = Configuration.getInstance();

            Properties properties = new Properties();
            properties.put("mail.store.protocol", "imap");
            properties.put("mail.imap.host", config.get("mail.imap.host"));
            properties.put("mail.imap.ssl.enable", true);
            properties.put("mail.imap.connectiontimeout", 6000);
            properties.put("mail.imap.timeout", 3000);
            properties.put("mail.imap.writetimeout", 3000);

            Session session = Session.getInstance(properties);
            // session.setDebug(true);

            // store = session.getStore();

            store = new SyncIMAPStore(session, null);
            store.connect(config.get("mail.imap.username"), config.get("mail.imap.password"));

            return store;
        } catch (MessagingException e) {
            throw new Error(e);
        }
    }

    public void teardown() {
        try {
            store.close();
        } catch (MessagingException e) {
        }
        folders.clear();
        messageCache.cleanUp();
    }

    public synchronized <T> T doQuery(Query<Store, T> func) {
        try {
            return func.query(store);
        } catch (Exception e) {
            if (store.isConnected()) {
                throw new Error(e);
            }
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
        }

        // Reconnect and retry
        log.info("Connection lost. Reconnecting.");
        teardown();
        store = createStore();

        try {
            return func.query(store);
        } catch (Exception e) {
            if (e instanceof RuntimeException)
                throw (RuntimeException) e;
            throw new Error(e);
        }
    }

    public <T> T doFolderQuery(String folder, Query<Folder, T> func) {
        return doQuery(store -> {
            Folder jfolder = getFolders(store).get(folder);
            return func.query(jfolder);
        });
    }

    public <T> T doMessageQuery(String message, Query<Message, T> func) {
        return doQuery(store -> {
            log.info("Loading message " + message);
            Message msg = messageCache.get(message, () -> {
                log.info("Cache miss for message " + message);

                int last = message.lastIndexOf('/');
                String folderName = message.substring(0, last);
                String idInFolder = message.substring(last + 1);
                return doFolderQuery(folderName, f -> {
                    if (!f.isOpen())
                        f.open(Folder.READ_WRITE);

                    Message m = f.getMessage(Integer.parseInt(idInFolder));
                    if (m instanceof IMAPMessage) {
                        ((IMAPMessage) m).setPeek(true);
                    }
                    return m;
                });
            });
            return func.query(msg);
        });
    }

    /*
     * Force reloading folders from the server
     */
    private LinkedHashMap<String, Folder> reloadFolders(Store store) throws MessagingException {
        LinkedHashMap<String, Folder> folders = new LinkedHashMap<>();

        // XXX: Change to a configurable value
        String altRoot = "[Gmail]";

        Folder f = store.getDefaultFolder();

        for (Folder subfolder : f.list("*")) {
            String fullname = subfolder.getFullName();
            String name = fullname;

            // INBOX is the special folder, so we deal it specially
            if (name.equals("INBOX")) {
                name = "Inbox";
            } else if (name.startsWith("INBOX/")) {
                name = "Inbox/" + name.substring("INBOX/".length());
            } else {
                // We may want to have an alternative root folder
                // E.g. Gmail creates a folder [Gmail] for system folders
                if (!altRoot.isEmpty()) {
                    if (name.equals(altRoot)) {
                        // Do not process on root folder
                        continue;
                    }

                    if (name.startsWith(altRoot + "/")) {
                        name = name.substring(altRoot.length() + 1);
                    }
                }
            }

            if (folders.containsKey(name)) {
                if (folders.containsKey(fullname)) {
                    throw new Error("Folder name duplication!");
                } else {
                    // Name existing already, force to revert
                    name = fullname;
                }
            }

            folders.put(name, subfolder);
        }

        return folders;
    }

    public LinkedHashMap<String, Folder> getFolders(Store store) throws MessagingException {
        if (foldersLastUpdate + 60000 >= System.currentTimeMillis()) {
            return this.folders;
        }

        log.info("Folder cache expired. Reloading");

        LinkedHashMap<String, Folder> folders = reloadFolders(store);

        // Remove disappeared folders from cache
        this.folders.keySet().retainAll(folders.keySet());

        // Add newly appeared folders to cache
        folders.keySet().removeAll(this.folders.keySet());
        this.folders.putAll(folders);

        messageCache.invalidateAll();

        foldersLastUpdate = System.currentTimeMillis();
        return this.folders;
    }

}
