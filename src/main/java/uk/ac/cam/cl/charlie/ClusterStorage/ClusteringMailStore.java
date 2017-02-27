package uk.ac.cam.cl.charlie.ClusterStorage;

import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.clustering.EMClusterer;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableObject;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;

import javax.mail.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by M Boyce on 27/02/2017.
 */
public class ClusteringMailStore extends Store {
    private Store store;
    private boolean valid;
    Map<String, VirtualFolder> virtualFolders = new HashMap<>();
    /**
     * Constructor.
     *
     * @param    session Session object for this Store.
     * @param    urlname    URLName object to be used for this Store
     */
    protected ClusteringMailStore(Session session, URLName urlname,Store store) {
        super(session, urlname);
        this.store = store;
        valid = false;
    }

    public void beginCluster() throws MessagingException {
        Folder inbox = store.getFolder("Inbox");
        ArrayList<Message> messages = new ArrayList<>();
        for(int i=0; i < inbox.getMessageCount(); i++)
            messages.add(inbox.getMessage(i));

        Clusterer clusterer = new EMClusterer(messages);

        ClusterGroup clusters = clusterer.getClusters();
        for(int i = 0; i < clusters.size(); i++){
            VirtualFolder virtualFolder = new VirtualFolder(store);
            Cluster cluster = clusters.get(i);

            virtualFolder.setFolderName(cluster.getName());
            ArrayList<ClusterableObject> clusteredObjects = cluster.getContents();
            Message[] clusteredMessages = new Message[clusteredObjects.size()];
            for(int j = 0; j < clusteredObjects.size(); j++)
                clusteredMessages[i] = ((ClusterableMessage)clusteredObjects.get(i)).getMessage();
            virtualFolder.appendMessages(clusteredMessages);
            virtualFolders.put(virtualFolder.getName(),virtualFolder);
        }
        valid = true;
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
