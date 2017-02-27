package uk.ac.cam.cl.charlie.ui;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import uk.ac.cam.cl.charlie.db.Configuration;

/**
 * Central class for handling requests and actions
 *
 * @author Gary Guo
 *
 */
public class Client {

    private static Client instance;

    private CachedStore cstore = new CachedStore();

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
        if (key.contains("password")) {
            String ret = Configuration.getInstance().get(key);
            if (ret == null) {
                return null;
            } else {
                return "(password)";
            }
        }
        return Configuration.getInstance().get(key);
    }

    public CachedStore getStore() {
        return cstore;
    }

}
