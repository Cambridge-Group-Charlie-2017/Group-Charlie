package uk.ac.cam.cl.charlie.clustering.store;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.clustering.EMClusterer;
import uk.ac.cam.cl.charlie.clustering.IncompatibleDimensionalityException;
import uk.ac.cam.cl.charlie.clustering.clusterNaming.ClusterNamer;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;
import uk.ac.cam.cl.charlie.clustering.clusters.EMCluster;
import uk.ac.cam.cl.charlie.db.Database;
import uk.ac.cam.cl.charlie.db.PersistentMap;
import uk.ac.cam.cl.charlie.db.Serializers;
import uk.ac.cam.cl.charlie.mail.Messages;
import uk.ac.cam.cl.charlie.ui.Client;
import uk.ac.cam.cl.charlie.vec.tfidf.TfidfVectoriser;

public class ClusteredFolder extends Folder {

    Folder actual;
    Map<String, VirtualFolder> virtualFolders = new HashMap<>();
    PersistentMap<Long, String> clusterMap;
    ClusterGroup<Message> clusterGroup;
    long lastUid;

    private static final int MESSAGE_COUNT_THRESHOLD = 100;
    private static final int MAX_COUNT_TO_CLUSTER = 500;

    Thread daemonThread;
    BlockingQueue<MessageCountEvent> queue = new LinkedBlockingQueue<>();
    boolean stopDaemon = false;

    private static Logger log = LoggerFactory.getLogger(ClusteredFolder.class);

    public ClusteredFolder(ClusteredStore store, Folder actual) {
        super(store);
        this.actual = actual;

        MessageCountListener messageListener = new MessageCountListener() {
            @Override
            public void messagesAdded(MessageCountEvent e) {
                queue.add(e);
            }

            @Override
            public void messagesRemoved(MessageCountEvent e) {
                // TODO: Should remove message from cluster here
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

        loadClusterGroup();
        reloadVirtualFolders();

        startDaemon();
    }

    private void startDaemon() {
        log.info("Starting daemon");
        stopDaemon = false;
        daemonThread = new Thread(this::run);
        daemonThread.setName("ClusteredFolder Daemon");
        daemonThread.setDaemon(true);
        daemonThread.start();
    }

    private void stopDaemon() {
        log.info("Stopping daemon");
        stopDaemon = true;
        daemonThread.interrupt();
        try {
            daemonThread.join();
        } catch (InterruptedException e) {
        }
    }

    private void runEmpty() {
        try {
            while (!stopDaemon) {
                // Once we have more message than the threshold,
                // we do initial clustering and complete
                if (actual.getMessageCount() > MESSAGE_COUNT_THRESHOLD) {
                    initialClustering();
                    return;
                }

                // Take an element from queue and discard
                try {
                    queue.take();
                } catch (InterruptedException e) {
                    continue;
                }
            }
        } catch (MessagingException e) {
            throw new Error(e);
        }
    }

    private void initialClustering() {
        TfidfVectoriser.getVectoriser().empty();
        try {
            int cnt = actual.getMessageCount();
            Message[] messages = actual.getMessages(Math.max(1, cnt - MAX_COUNT_TO_CLUSTER), cnt);
            ArrayList<Message> msg = new ArrayList<>(Arrays.asList(messages));
            log.info("{}: Downloading messages", actual.getName());
            try {
                Iterator<Message> iter = msg.iterator();
                while (iter.hasNext()) {
                    Message m = iter.next();
                    // If size is too large, we skip it to save time
                    if (m.getSize() >= 65536) {
                        iter.remove();
                    } else {
                        m.getContent();
                    }
                }
            } catch (MessagingException | IOException e) {
                e.printStackTrace();
                return;
            }
            log.info("{}: Messages downloaded", actual.getName());

            Clusterer.getVectoriser().train(msg);

            log.info("{}: Start clustering", actual.getName());

            EMClusterer<Message> cluster = new EMClusterer<>();
            ClusterGroup<Message> clusters = cluster
                    .cluster(msg.stream().map(m -> new ClusterableMessage(m)).collect(Collectors.toList()));

            log.info("{}: Clustered, start naming", actual.getName());

            for (Cluster<Message> c : clusters) {
                ClusterNamer.doName(c);
            }

            Queue<Cluster<Message>> queue = new CircularFifoQueue<>();
            for (Cluster<Message> c : clusters)
                queue.add(c);

            int loop = 0;

            ClusterGroup<Message> toAdd = new ClusterGroup<>();
            boolean clear = true;
            while (!queue.isEmpty() && loop < 30) {
                Cluster<Message> c = queue.remove();
                ClusterGroup<Message> subclusters;
                if (!c.getNameConfidence() && c.getSize() > 30) {
                    log.info("Spliting cluster: {}", c.getName());
                    EMClusterer<Message> clusterer = new EMClusterer<>(2);
                    subclusters = clusterer.cluster(
                            c.getObjects().stream().map(m -> (ClusterableMessage) m).collect(Collectors.toList()));
                    Iterator<Cluster<Message>> iterator = subclusters.iterator();
                    while (iterator.hasNext()) {
                        Cluster<Message> subCluster = iterator.next();
                        ClusterNamer.doName(subCluster);
                        if (!subCluster.getNameConfidence() && subCluster.getSize() > 30 && loop < 30) {
                            queue.add(subCluster);
                            iterator.remove();
                            loop++;
                        }
                    }

                    for (Cluster<Message> subcluster : subclusters) {
                        log.info("Split into: {}", subcluster.getName());
                        toAdd.add(subcluster);
                    }

                    if (clear)
                        clear = false;
                } else {
                    try {
                        toAdd.add(c);
                    } catch (IncompatibleDimensionalityException e) {
                        e.printStackTrace();
                    }
                }
            }

            for (Cluster<Message> clust : queue)
                toAdd.add(clust);

            log.info("{}: Naming completed", actual.getName());

            addClusters(toAdd);
        } catch (MessagingException e) {
            throw new Error(e);
        }
    }

    private void run() {
        if (clusterMap.isEmpty()) {
            runEmpty();
        }

        // Might be new messages after initialization of Folder but before we
        // register the listener
        checkNewMessage();

        while (!stopDaemon) {
            MessageCountEvent event;
            try {
                event = queue.take();
            } catch (InterruptedException e) {
                continue;
            }
            if (event.getType() == MessageCountEvent.ADDED) {
                Message[] newMessages = event.getMessages();
                processNewMessage(newMessages);
            }
        }
    }

    private void loadClusterGroup() {
        clusterGroup = new ClusterGroup<>();

        HashMap<String, ArrayList<ClusterableObject<Message>>> map = new HashMap<>();

        // Load all messages to map
        for (Entry<Long, String> entry : clusterMap.entrySet()) {
            Message message;
            try {
                message = ((UIDFolder) actual).getMessageByUID(entry.getKey());
            } catch (MessagingException e) {
                e.printStackTrace();
                continue;
            }
            if (message == null) {
                continue;
            }

            ArrayList<ClusterableObject<Message>> list = map.get(entry.getValue());

            if (list == null) {
                list = new ArrayList<>();
                map.put(entry.getValue(), list);
            }

            list.add(new ClusterableMessage(message));

            lastUid = entry.getKey();
        }

        for (Entry<String, ArrayList<ClusterableObject<Message>>> entry : map.entrySet()) {
            EMCluster<Message> cluster = new EMCluster<>(entry.getValue());
            cluster.setName(entry.getKey());
            clusterGroup.add(cluster);
        }
    }

    private void reloadVirtualFolders() {
        virtualFolders.clear();
        for (Cluster<Message> cluster : clusterGroup) {
            VirtualFolder vf = new VirtualFolder(getStore(), this, cluster);
            virtualFolders.put(vf.getName(), vf);
        }
    }

    private void addClusters(ClusterGroup<Message> clusterGroup) {
        clusterMap.clear();
        ClusterGroup<Message> mergedClusterGroup = new ClusterGroup<>();
        Map<String, Cluster<Message>> clusterNameToCluster = new HashMap<>();
        // Merge clusters with the same name
        for (Cluster<Message> c : clusterGroup) {
            // If cluster name already exists merge clusters
            if (clusterNameToCluster.containsKey(c.getName())) {
                Cluster<Message> existingCluster = clusterNameToCluster.get(c.getName());
                for (ClusterableObject<Message> m : c.getObjects())
                    existingCluster.addObject(m);
            } else {// Add as new cluster
                clusterNameToCluster.put(c.getName(), c);
            }
        }
        Iterator<Cluster<Message>> iterator = clusterNameToCluster.values().iterator();
        while (iterator.hasNext())
            mergedClusterGroup.add(iterator.next());

        this.clusterGroup = mergedClusterGroup;

        for (Cluster<Message> cluster : mergedClusterGroup) {
            for (ClusterableObject<Message> obj : cluster.getObjects()) {
                Message msg = obj.getObject();
                long uid;
                try {
                    uid = ((UIDFolder) msg.getFolder()).getUID(msg);
                } catch (MessagingException e) {
                    throw new Error(e);
                }
                if (uid > lastUid)
                    lastUid = uid;
                clusterMap.put(uid, cluster.getName());
            }
        }

        reloadVirtualFolders();

        Client.getInstance().reload();
    }

    private void checkNewMessage() {
        Message[] newMessages;
        try {
            newMessages = ((UIDFolder) actual).getMessagesByUID(lastUid + 1, UIDFolder.LASTUID);
        } catch (MessagingException e) {
            e.printStackTrace();
            return;
        }
        processNewMessage(newMessages);
    }

    private void processNewMessage(Message[] messages) {
        for (Message m : messages) {
            long uid;
            try {
                // Too large, may took a while to download
                if (m.getSize() > 65535) {
                    continue;
                }

                uid = Messages.getUID(m);
            } catch (MessagingException e) {
                throw new Error(e);
            }
            log.info("Classifying message {}", uid);
            String name = clusterGroup.insert(new ClusterableMessage(m));
            clusterMap.put(uid, name);
            log.info("Classified message {} into {}", uid, name);
        }
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

    public ClusterGroup<Message> getClusterGroup() {
        return clusterGroup;
    }

    public void recluster() {
        stopDaemon();
        log.info("Start reclustering");
        initialClustering();
        startDaemon();
    }
}
