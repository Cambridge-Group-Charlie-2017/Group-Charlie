package uk.ac.cam.cl.charlie.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class IOUtils {

    public static void pipe(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[4096];
        int count = 0;
        while ((count = is.read(buffer)) != -1) {
            os.write(buffer, 0, count);
        }
    }

    public static void save(InputStream is, String file) throws IOException {
        try (FileOutputStream os = new FileOutputStream(file)) {
            pipe(is, os);
        }
    }

    public static void save(InputStream is, File file) throws IOException {
        try (FileOutputStream os = new FileOutputStream(file)) {
            pipe(is, os);
        }
    }

    public static byte[] readBytes(InputStream is) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        pipe(is, os);
        return os.toByteArray();
    }

    public static String readString(InputStream is) throws IOException {
        return new String(readBytes(is), "UTF-8");
    }

}
