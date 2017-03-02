package uk.ac.cam.cl.charlie.clustering.store;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.Map.Entry;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import uk.ac.cam.cl.charlie.clustering.IncompatibleDimensionalityException;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;
import uk.ac.cam.cl.charlie.clustering.clusters.EMCluster;
import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializers;

public class ClusteredFolder extends Folder {

    Folder actual;
    Map<String, VirtualFolder> virtualFolders = new HashMap<>();
    PersistentMap<Long, String> clusterMap;
    ClusterGroup clusterGroup;
    private ArrayList<Message> newMessages = new ArrayList<>();


    public ClusteredFolder(ClusteredStore store, Folder actual) {
        super(store);
        this.actual = actual;

        MessageCountListener messageListener = new MessageCountListener() {
            @Override
            public void messagesAdded(MessageCountEvent e) {
                //add to new messages
                Message[] msgs = e.getMessages();
                for (Message m : msgs)
                    newMessages.add(m);
            }

            @Override
            public void messagesRemoved(MessageCountEvent e) {
                //TODO: Should anything be done here?
            }
        };
        actual.addMessageCountListener(messageListener);

        try {
            clusterMap = Database.getInstance().getMap(
                    "cluster-" + Base64.getUrlEncoder().encodeToString(actual.getFullName().getBytes("UTF-8")),
                    Serializers.LONG, Serializers.STRING);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }

        load();
        initNewMailChecker();
    }

    private void load() {
        virtualFolders.clear();
        clusterGroup = new ClusterGroup();

        //load virtualFolders
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

        //load clusterGroup
        HashMap<String, ArrayList<ClusterableMessage>> map = new HashMap<>();
        for (Entry<Long, String> entry : clusterMap.entrySet()) {
            if (!map.containsKey(entry.getValue()))
                map.put(entry.getValue(), new ArrayList<>());
            try {
                map.get(entry.getValue()).add(new ClusterableMessage(((UIDFolder) actual).getMessageByUID(entry.getKey())));
            }   catch (MessagingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        for (String s : map.keySet()) {
            try {
                clusterGroup.add(new EMCluster(map.get(s)));
            } catch (IncompatibleDimensionalityException e) {
                e.printStackTrace();
            }
        }
    }

    public void addClusters(ClusterGroup<Message> clusterGroup) {
        clusterMap.clear();
        this.clusterGroup = clusterGroup;

        for (Cluster<Message> cluster : clusterGroup) {
            for (ClusterableObject<Message> obj : cluster.getObjects()) {
                Message msg = obj.getObject();
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

    public void refreshMessages() {
        if (clusterGroup.size() == 0) {
            return; //can't classify when no clusters exist.
        }
        for (Message m : newMessages) {
            String name;
            try {
                //insert into clusterGroup and into the corresponding virtualFolder.
                name = clusterGroup.insert(new ClusterableMessage(m));
                virtualFolders.get(name).addMessage(m);
            } catch (IncompatibleDimensionalityException e) {
                e.printStackTrace();
            }
        }
        //refresh ArrayList, all new messages now dealt with.
        newMessages = new ArrayList<>();
    }

    public void initNewMailChecker() {
        Thread newMailTask = new Thread() {
            public void run() {

                while(true) {
                    try {
                        //sleep 1 min then classify new emails
                        sleep(60000);
                        refreshMessages();
                    } catch (InterruptedException e) {
                        //shouldn't occur.
                        e.printStackTrace();
                        throw new Error(e);
                    }
                }
            }
        };
        newMailTask.setDaemon(true);
        newMailTask.start();
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
