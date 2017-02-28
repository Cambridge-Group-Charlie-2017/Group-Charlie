package uk.ac.cam.cl.charlie.ClusterStorage;

import uk.ac.cam.cl.charlie.clustering.ClusterableMessageGroup;
import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.clustering.EMClusterer;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;

import javax.mail.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by M Boyce on 27/02/2017.
 */
public class ClusteringMailStore extends Store {
    private Store store;
    private boolean valid;
    private Map<String, VirtualFolder> virtualFolders = new HashMap<>(); //map from folder name to folder
    private PersistantIDStore table = new PersistantIDStore();
    private VirtualFolder root = VirtualFolder.getRoot(this);
    /**
     * Constructor.
     *
     * @param    session Session object for this Store.
     * @param    urlname    URLName object to be used for this Store
     */
    protected ClusteringMailStore(Session session, URLName urlname, Store store) {
        super(session, urlname);
        this.store = store;
        valid = false;
        String[] folderNames;
        try {
            folderNames = table.getVals();
        } catch (IOException e) {
            throw new Error(e);
        }

        //import current folder structure from database
        for (String val : folderNames) {
            VirtualFolder folder = new VirtualFolder(this, root);

            int[] msgIDs;
            try {
                msgIDs = table.getAllWithvalue(val);
            } catch (IOException e) {
                throw new Error(e);
            }
            ArrayList<Message> messages = new ArrayList<>();
            for (int id : msgIDs) {
                try {
                    messages.add(store.getFolder("Inbox").getMessage(id));
                } catch (MessagingException e) {
                    throw new Error(e);
                }
            }
            Message[] messageArr = new Message[messages.size()];
            messageArr = messages.toArray(messageArr);
            try {
                folder.appendMessages(messageArr);
            } catch (MessagingException e) {
                throw new Error(e);
            }
            virtualFolders.put(val, folder);
        }
    }

    //Drunk when coding this function. Probably a good idea to double check it.
    //Wipe current clusters, replaces with new cluster group.
    public void addClusters(ClusterGroup clusterGroup) {
        if (clusterGroup.size() == 0)
            return;
        if (!(clusterGroup.get(0).getContents().get(0) instanceof ClusterableMessage))
            return;

        try {
            table.wipeDatabase();
        } catch (IOException e) {
            throw new Error(e);
        }

        ArrayList<Message> messages = new ArrayList<>();
        ArrayList<String> clusterNameList = new ArrayList<>();
        for (Cluster cluster : clusterGroup) {
            ArrayList<ClusterableObject> objects = cluster.getContents();
            for (ClusterableObject obj : objects) {
                messages.add(((ClusterableMessage) obj).getMessage());
                clusterNameList.add(cluster.getName());
            }
        }

        int[] messageIDs = new int[messages.size()];
        String[] clusterNames = new String[messages.size()];
        for (int i = 0; i < messages.size(); i++)
            messageIDs[i] = messages.get(i).getMessageNumber();

        clusterNames = clusterNameList.toArray(clusterNames);

        try {
            table.batchInsert(messageIDs, clusterNames);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public void delete(Message msg) {
        table.delete(msg.getMessageNumber());
        if (virtualFolders.containsValue(msg.getFolder())) {
            //TODO: is this the correct way?
            ((VirtualFolder) msg.getFolder()).removeMessage(msg);
            return;
        }
    }

    //Only valid if message is already in a cluster (not a user-created folder).
    public void move(Message msg, String clusterName) {
        table.move(msg.getMessageNumber(), clusterName);
        Message[] singleMessageArray = {msg};
        try {
            delete(msg);
            virtualFolders.get(clusterName).appendMessages(singleMessageArray);
        } catch (MessagingException e) {
            throw new Error(e);
        }

        //TODO: If clusterName exists on IMAP structure move there? Then only call delete(), not move()
    }

    public void addMessage(Message msg, String clusterName) {
        Message[] singleMessage = {msg};
        addMessagesToCluster(singleMessage, clusterName);
    }

    public void addMessagesToCluster(Message[] messages, String clusterName) {
        VirtualFolder folder = virtualFolders.get(clusterName); //TODO: handle if clustername is invalid
        for (int i  = 0; i < messages.length; i++) {
            table.insert(messages[i].getMessageNumber(), clusterName);
        }
        try {
            folder.appendMessages(messages);
        } catch (MessagingException e) {
            throw new Error(e);
        }

        //TODO: Possibly if clusterName exists on IMAP structure add to there?
    }



    @Override
    public Folder getDefaultFolder() throws MessagingException {
        return store.getDefaultFolder();
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        if(valid) {
            if (name == "Inbox")
                return store.getFolder("Inbox");
            else if (virtualFolders.containsKey(name)) //Is a recalculated cluster
                return virtualFolders.get(name);
            else
                return store.getFolder(name); //User-created folder
        }else{
            return store.getFolder(name);
        }
    }

    @Override
    public Folder getFolder(URLName url) throws MessagingException {
        return null; //not applicable.
    }
}
