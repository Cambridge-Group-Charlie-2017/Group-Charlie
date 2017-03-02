package uk.ac.cam.cl.charlie.ui;

import java.awt.Dialog;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.platform.win32.User32;

import uk.ac.cam.cl.charlie.os.win.ComDlg32;
import uk.ac.cam.cl.charlie.util.OS;

public class FileChooser {

    private Window window;
    private String title;
    private String file;

    public FileChooser parent(Window window) {
        this.window = window;
        return this;
    }

    public FileChooser title(String title) {
        this.title = title;
        return this;
    }

    public FileChooser file(String file) {
        this.file = file;
        return this;
    }

    private File performWin32(boolean open) {
        ComDlg32.OpenFileName params = new ComDlg32.OpenFileName();

        params.Flags = ComDlg32.OpenFileName.OFN_EXPLORER | ComDlg32.OpenFileName.OFN_NOCHANGEDIR
                | ComDlg32.OpenFileName.OFN_HIDEREADONLY | ComDlg32.OpenFileName.OFN_ENABLESIZING;

        if (window != null) {
            params.hwndOwner = Native.getWindowPointer(window);
        } else {
            params.hwndOwner = User32.INSTANCE.GetForegroundWindow().getPointer();
        }

        if (title != null) {
            params.lpstrTitle = new WString(title);
        }

        // Set up output buffer
        int bufferLength = 260;
        int bufferSize = 4 * (bufferLength + 1);
        Memory buffer = new Memory(bufferSize);
        buffer.clear();

        if (file != null) {
            buffer.setWideString(0, file);
        }
        params.lpstrFile = buffer;
        params.nMaxFile = bufferLength;

        boolean result = open ? ComDlg32.GetOpenFileNameW(params) : ComDlg32.GetSaveFileNameW(params);
        if (result) {
            String path = params.lpstrFile.getWideString(0);
            return new File(path);
        }

        return null;
    }

    private File perform(boolean open) {
        if (OS.isWindows()) {
            return performWin32(open);
        }

        String title = this.title != null ? this.title : open ? "Open" : "Save As";
        int flag = open ? FileDialog.LOAD : FileDialog.SAVE;

        FileDialog fd;
        if (window instanceof Frame) {
            fd = new FileDialog((Frame) window, title, flag);
        } else if (window instanceof Dialog) {
            fd = new FileDialog((Dialog) window, title, flag);
        } else {
            fd = new FileDialog((Frame) null, title, flag);
        }

        if (file != null) {
            fd.setFile(file);
        }

        fd.setVisible(true);
        File[] filename = fd.getFiles();
        if (filename.length != 0) {
            return filename[0];
        } else {
            return null;
        }
    }

    public File open() {
        return perform(true);
    }

    public File save() {
        return perform(false);
    }

}
