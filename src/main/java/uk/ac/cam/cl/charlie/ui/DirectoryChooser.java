package uk.ac.cam.cl.charlie.ui;

import java.awt.Window;
import java.io.File;

import javax.swing.JFileChooser;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.User32;

import uk.ac.cam.cl.charlie.os.win.Shell32;
import uk.ac.cam.cl.charlie.util.OS;

public class DirectoryChooser {

    private Window window;
    private String title;
    private File directory;

    public DirectoryChooser parent(Window window) {
        this.window = window;
        return this;
    }

    public DirectoryChooser title(String title) {
        this.title = title;
        return this;
    }

    public DirectoryChooser directory(File directory) {
        this.directory = directory;
        return this;
    }

    private File performWin32() {
        Ole32.INSTANCE.CoInitialize(null);

        Shell32.BrowseInfo lpbi = new Shell32.BrowseInfo();

        if (window != null) {
            lpbi.hwndOwner = Native.getWindowPointer(window);
        } else {
            lpbi.hwndOwner = User32.INSTANCE.GetForegroundWindow().getPointer();
        }

        lpbi.ulFlags = Shell32.BIF_RETURNONLYFSDIRS | Shell32.BIF_USENEWUI;

        if (title != null) {
            lpbi.lpszTitle = new WString(title);
        } else {
            lpbi.lpszTitle = new WString("Select the directory");
        }

        Pointer pidl = Shell32.SHBrowseForFolderW(lpbi);
        if (pidl != null) {
            int bufferLength = 260;
            int bufferSize = 4 * (bufferLength + 1);
            Memory buffer = new Memory(bufferSize);

            Shell32.SHGetPathFromIDListW(pidl, buffer);
            String filePath = buffer.getWideString(0);
            File file = new File(filePath);
            Ole32.INSTANCE.CoTaskMemFree(pidl);
            return file;
        }
        return null;
    }

    private File perform() {
        if (OS.isWindows()) {
            return performWin32();
        }

        JFileChooser fd = new JFileChooser();

        String title = this.title != null ? this.title : "Select Directory";
        fd.setDialogTitle(title);

        if (directory != null) {
            fd.setCurrentDirectory(directory);
        }

        fd.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fd.setAcceptAllFileFilterUsed(false);

        int result = fd.showOpenDialog(window);
        if (result == JFileChooser.APPROVE_OPTION) {
            return fd.getSelectedFile();
        } else {
            return null;
        }
    }

    public File open() {
        return perform();
    }

}
