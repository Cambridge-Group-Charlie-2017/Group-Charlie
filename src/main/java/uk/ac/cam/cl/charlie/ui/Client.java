package uk.ac.cam.cl.charlie.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.mail.Message;
import javax.mail.MessagingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import uk.ac.cam.cl.charlie.clustering.Clusterer;
import uk.ac.cam.cl.charlie.clustering.EMClusterer;
import uk.ac.cam.cl.charlie.clustering.clusterNaming.ClusterNamer;
import uk.ac.cam.cl.charlie.clustering.clusterableObjects.ClusterableMessage;
import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;
import uk.ac.cam.cl.charlie.clustering.clusters.ClusterGroup;
import uk.ac.cam.cl.charlie.clustering.store.ClusteredFolder;
import uk.ac.cam.cl.charlie.db.Configuration;
import uk.ac.cam.cl.charlie.filewalker.BasicFileWalker;
import uk.ac.cam.cl.charlie.filewalker.FileDB;
import uk.ac.cam.cl.charlie.math.Vector;
import uk.ac.cam.cl.charlie.vec.Document;

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

    private BasicFileWalker walker = new BasicFileWalker();
    {
        for (String root : jsonListToStringList(getConfiguration("filewalker.root"))) {
            walker.addRootDirectory(Paths.get(root));
        }
    }

    private List<String> jsonListToStringList(String json) {
        if (json == null) {
            return Collections.emptyList();
        }
        JsonArray array = new JsonParser().parse(json).getAsJsonArray();
        List<String> list = new ArrayList<>(array.size());
        for (JsonElement e : array) {
            list.add(e.getAsString());
        }
        return list;
    }

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
        switch (key) {
        case "filewalker.root":
            updateFilewalkerRoot(value);
            break;
        }
        Configuration.getInstance().put(key, value);
    }

    private void updateFilewalkerRoot(String newValue) {
        List<String> oldValue = jsonListToStringList(getConfiguration("filewalker.root"));
        List<String> newList = jsonListToStringList(newValue);

        Set<String> removedElement = new HashSet<>(oldValue);
        removedElement.removeAll(newList);

        Set<String> newElement = new HashSet<>(newList);
        newElement.removeAll(oldValue);

        for (String s : newElement) {
            walker.addRootDirectory(Paths.get(s));
        }

        for (String s : removedElement) {
            walker.removeRootDirectory(Paths.get(s));
        }
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

        EMClusterer<Message> cluster = new EMClusterer<>(
                msg.stream().map(m -> new ClusterableMessage(m)).collect(Collectors.toList()));
        ClusterGroup<Message> group = cluster.getClusters();

        for (Cluster<Message> c : group) {
            ClusterNamer.doName(c);
        }

        cstore.doFolderQuery("Inbox", folder -> {
            ClusteredFolder cfolder = (ClusteredFolder) folder;
            cfolder.addClusters(group);
            // Invalidate folder cache
            cstore.foldersLastUpdate = 0;
            return null;
        });

    }

    public Optional<Path> getFileSuggestion(String text) {
        Document document = new Document(null, text);
        Vector vec = Clusterer.getVectoriser().doc2vec(document);

        return FileDB.getInstance().getMostRelevantFile(vec);
    }

}
