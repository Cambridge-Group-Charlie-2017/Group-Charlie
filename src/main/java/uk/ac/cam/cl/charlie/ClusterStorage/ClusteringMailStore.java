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
import java.util.HashMap;
import java.util.Map;

/**
 * Created by M Boyce on 27/02/2017.
 */
public class ClusteringMailStore extends Store {
    private Store store;
    private boolean valid;
    private Map<String, VirtualFolder> virtualFolders = new HashMap<>();
    private PersistantIDStore table = new PersistantIDStore();
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

    @Override
    public Folder getDefaultFolder() throws MessagingException {
        return store.getDefaultFolder();
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        if(valid) {
            if (name == "Inbox")
                return store.getFolder(name);
            else if (virtualFolders.containsKey(name))
                return virtualFolders.get(name);
            else
                return store.getFolder(name);
        }else{
            return store.getFolder(name);
        }
    }

    @Override
    public Folder getFolder(URLName url) throws MessagingException {
        return null;
    }
}
