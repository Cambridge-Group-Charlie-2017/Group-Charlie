package uk.ac.cam.cl.charlie.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;

import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.search.FlagTerm;

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
import uk.ac.cam.cl.charlie.clustering.store.ClusteredFolder;
import uk.ac.cam.cl.charlie.clustering.store.VirtualFolder;
import uk.ac.cam.cl.charlie.db.Configuration;

/**
 * Central class for handling requests and actions
 *
 * @author Gary Guo
 *
 */
public class Client {

    private static Client instance;

    private static Logger log = LoggerFactory.getLogger(WebUIServer.class);
    private CachedStore cstore;
    private ClusterGroup<Message> clusters;

    public static Client getInstance() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    public void launchBrowser() {
        if (Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().browse(new URI("http://localhost:" + WebUIServer.PORT));
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    /*
     * GET /api/settings/config/:key
     */
    public String getConfiguration(String key) {
        return Configuration.getInstance().get(key);
    }

    public CachedStore getStore() {
        if (cstore == null) {
            cstore = new CachedStore();
        }
        return cstore;
    }

    public void putConfiguration(String key, String value) {
        Configuration.getInstance().put(key, value);
    }

    public void changeAccount() {
        log.info("Changing account");
        if (cstore != null)
            cstore.teardown();
        cstore = new CachedStore();
    }

    public void startClustering() {
        if (cstore == null) {
            cstore = new CachedStore();
        }
        Message[] message = cstore.doFolderQuery("Inbox", folder -> {
            int cnt = folder.getMessageCount();
            return folder.getMessages(Math.max(1, cnt - 500), cnt);
        });
        ArrayList<Message> msg = new ArrayList<>(Arrays.asList(message));
        log.info("Start downloading all emails");
        try {
            Iterator<Message> iter = msg.iterator();
            while (iter.hasNext()) {
                Message m = iter.next();
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
        log.info("Downloaded all emails");

        Clusterer.getVectoriser().train(msg);

        clusters = cluster(msg.stream().map(m -> new ClusterableMessage(m)).collect(Collectors.toList()));
        
        /*createFolderForClusters("Inbox", clusters);
        
        for (Cluster<Message> c : clusters) {
        	ClusterGroup<Message> subclusters;
        	if(!c.getNameConfidence() && c.getSize() > 30) {
        		subclusters = cluster(c.getObjects());
        		createFolderForClusters("Inbox", subclusters);
            }
        }*/

        Queue<Cluster<Message>> queue = new CircularFifoQueue<>();
        for(Cluster<Message> c : clusters)
            queue.add(c);

        int loop = 0;

        ClusterGroup<Message> toAdd = new ClusterGroup<>();
        boolean clear = true;

        while(!queue.isEmpty() && loop < 30){
            Cluster<Message> c = queue.remove();
            ClusterGroup<Message> subclusters;
            if(!c.getNameConfidence() && c.getSize() > 30) {
                subclusters = cluster(c.getObjects());
                Iterator<Cluster<Message>> iterator = subclusters.iterator();
                while(iterator.hasNext()){
                    Cluster<Message> subCluster = iterator.next();
                    if(!subCluster.getNameConfidence() && subCluster.getSize() > 30 && loop<30) {
                        queue.add(subCluster);
                        iterator.remove();
                        loop++;
                    }
                }
                createFolderForClusters("Inbox", subclusters,clear);
                if(clear)
                    clear=false;
            }else{
                try {
                    toAdd.add(c);
                } catch (IncompatibleDimensionalityException e) {
                    e.printStackTrace();
                }
            }

            createFolderForClusters("Inbox",toAdd,clear);
        }


    }
    
    private ClusterGroup<Message> cluster(List<ClusterableObject<Message>> msg) {
    	ClusterGroup<Message> clusters;
    	EMClusterer<Message> cluster = new EMClusterer<>(msg);
        clusters = cluster.getClusters();

        for (Cluster<Message> c : clusters) {
            ClusterNamer.doName(c);
        }
    	return clusters;
    }
    
    private void createFolderForClusters(String parentFolder, ClusterGroup<Message> clusters, boolean clear) {
    	cstore.doFolderQuery(parentFolder, folder -> {
    	    if(folder instanceof  ClusteredFolder) {
                ClusteredFolder cfolder = (ClusteredFolder) folder;
                cfolder.addClusters(clusters,clear);
                // Invalidate folder cache
                cstore.foldersLastUpdate = 0;
            }
            return null;
        });
    }

}
