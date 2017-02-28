package uk.ac.cam.cl.charlie.clustering.store;

import java.io.UnsupportedEncodingException;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;

import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;
import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializers;

public class ClusteredFolder extends Folder {

    Folder actual;
    Map<String, VirtualFolder> virtualFolders = new HashMap<>();
    PersistentMap<Long, String> clusterMap;

    public ClusteredFolder(ClusteredStore store, Folder actual) {
        super(store);
        this.actual = actual;

        try {
            clusterMap = Database.getInstance().getMap(
                    "cluster-" + Base64.getUrlEncoder().encodeToString(actual.getFullName().getBytes("UTF-8")),
                    Serializers.LONG, Serializers.STRING);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }

        load();
    }

    private void load() {
        virtualFolders.clear();

        for (Entry<Long, String> entry : clusterMap.entrySet()) {
            VirtualFolder vf = virtualFolders.get(entry.getValue());
            if (vf == null) {
                vf = new VirtualFolder(getStore(), this, entry.getValue());
                virtualFolders.put(entry.getValue(), vf);
            }

            try {
                vf.addMessage(((UIDFolder) actual).getMessageByUID(entry.getKey()));
            } catch (MessagingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void addClusters(ClusterGroup clusterGroup) {
        clusterMap.clear();

        for (Cluster cluster : clusterGroup) {
            for (ClusterableObject obj : cluster.getContents()) {
                Message msg = ((ClusterableMessage) obj).getMessage();
                long uid;
                try {
                    uid = ((UIDFolder) msg.getFolder()).getUID(msg);
                } catch (MessagingException e) {
                    throw new Error(e);
                }
                clusterMap.put(uid, cluster.getName());
            }
        }

        load();
    }

    @Override
    public String getName() {
        return actual.getName();
    }

    @Override
    public String getFullName() {
        return actual.getFullName();
    }

    @Override
    public Folder getParent() throws MessagingException {
        // Instead of getting parent directly, we request it from the store
        return store.getFolder(actual.getParent().getFullName());
    }

    @Override
    public boolean exists() throws MessagingException {
        return actual.exists();
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        if (pattern.equals("%")) {
            Collection<VirtualFolder> collection = this.virtualFolders.values();
            return collection.toArray(new Folder[collection.size()]);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public char getSeparator() throws MessagingException {
        return actual.getSeparator();
    }

    @Override
    public int getType() throws MessagingException {
        return actual.getType();
    }

    @Override
    public boolean create(int type) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        return virtualFolders.get(name);
    }

    @Override
    public boolean delete(boolean recurse) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean renameTo(Folder f) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void open(int mode) throws MessagingException {
        actual.open(mode);
    }

    @Override
    public void close(boolean expunge) throws MessagingException {
        actual.close(expunge);
    }

    @Override
    public boolean isOpen() {
        return actual.isOpen();
    }

    @Override
    public Flags getPermanentFlags() {
        return actual.getPermanentFlags();
    }

    @Override
    public int getMessageCount() throws MessagingException {
        return actual.getMessageCount();
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        return actual.getMessage(msgnum);
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        actual.appendMessages(msgs);
    }

    @Override
    public Message[] expunge() throws MessagingException {
        return actual.expunge();
    }

}
