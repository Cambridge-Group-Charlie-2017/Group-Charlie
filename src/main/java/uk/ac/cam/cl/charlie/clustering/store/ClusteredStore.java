package uk.ac.cam.cl.charlie.clustering.store;

import java.util.HashMap;
import java.util.Map;

import javax.mail.Folder;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

/**
 * @author M Boyce
 * @author Gary Guo
 */
public class ClusteredStore extends Store {
    private Store store;
    protected Map<String, Folder> folders = new HashMap<>();

    /**
     * Constructor.
     *
     * @param session
     *            Session object for this Store.
     * @param urlname
     *            URLName object to be used for this Store
     */
    public ClusteredStore(Session session, URLName urlname, Store store) {
        super(session, urlname);
        this.store = store;
    }

    private void loadFoldersFromStore() throws MessagingException {
        Folder[] folders = store.getDefaultFolder().list("*");
        this.folders.put("", new UnclusteredFolder(this, store.getDefaultFolder()));
        for (Folder f : folders) {
            if (f.getFullName().equals("INBOX")) {
                this.folders.put(f.getFullName(), new ClusteredFolder(this, f));
            } else {
                this.folders.put(f.getFullName(), new UnclusteredFolder(this, f));
            }
        }
    }

    @Override
    public Folder getDefaultFolder() throws MessagingException {
        return getFolder("");
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        if (name.startsWith("INBOX/")) {
            return getFolder("INBOX").getFolder(name.substring("INBOX/".length()));
        }
        return folders.get(name);
    }

    @Override
    public Folder getFolder(URLName url) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean protocolConnect(String host, int port, String user, String password) throws MessagingException {
        store.connect(host, port, user, password);

        try {
            loadFoldersFromStore();
        } catch (MessagingException e) {
            e.printStackTrace();
        }
        return true;
    }

}
