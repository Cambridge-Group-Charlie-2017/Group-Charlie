package uk.ac.cam.cl.charlie.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;

public class Wallpaper {

    private static boolean USE_SYSTEM_WALLPAPER = true;

    public static String getWallpaper() {
        if (!USE_SYSTEM_WALLPAPER)
            return null;
        if (OS.isWindows()) {
            byte[] bytes = Advapi32Util.registryGetBinaryValue(WinReg.HKEY_CURRENT_USER, "Control Panel\\Desktop",
                    "TranscodedImageCache");
            if (bytes == null || bytes.length <= 24)
                return null;
            String name;
            try {
                name = new String(bytes, 24, bytes.length - 24, "UTF-16LE").trim();
            } catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
            return name;
        } else if (OS.isLinux()) {
            try {
                Process process = Runtime.getRuntime()
                        .exec(new String[] { "gsettings", "get", "org.gnome.desktop.background picture-uri" });
                String url = IOUtils.readString(process.getInputStream());
                if (url.isEmpty()) {
                    return null;
                }
                return new URL(url).getPath();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }

}
