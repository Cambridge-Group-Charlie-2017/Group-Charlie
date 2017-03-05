package uk.ac.cam.cl.charlie.os.win;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;

/**
 * Native bindings of Windows's Shell32.dll, see <a href=
 * "https://msdn.microsoft.com/en-us/library/windows/desktop/ff521731(v=vs.85).aspx">https://msdn.microsoft.com/en-us/library/windows/desktop/ff521731(v=vs.85).aspx</a>
 *
 * @author Gary Guo
 */
public class Shell32 {

    /**
     * BROWSEINFO structure used in Shell32.dll, see <a href=
     * "https://msdn.microsoft.com/en-us/library/windows/desktop/bb773205(v=vs.85).aspx">https://msdn.microsoft.com/en-us/library/windows/desktop/bb773205(v=vs.85).aspx</a>
     *
     * @author Gary Guo
     */
    public static class BrowseInfo extends Structure {
        public Pointer hwndOwner;
        public Pointer pidlRoot;
        public WString pszDisplayName;
        public WString lpszTitle;
        public int ulFlags;
        public Pointer lpfn;
        public Pointer lParam;
        public int iImage;

        @Override
        protected List<String> getFieldOrder() {
            return Arrays.asList(new String[] { "hwndOwner", "pidlRoot", "pszDisplayName", "lpszTitle", "ulFlags",
                    "lpfn", "lParam", "iImage" });
        }
    }

    public static final int BIF_RETURNONLYFSDIRS = 0x00000001;
    public static final int BIF_DONTGOBELOWDOMAIN = 0x00000002;
    public static final int BIF_STATUSTEXT = 0x00000004;
    public static final int BIF_RETURNFSANCESTORS = 0x00000008;
    public static final int BIF_EDITBOX = 0x00000010;
    public static final int BIF_VALIDATE = 0x00000020;
    public static final int BIF_NEWDIALOGSTYLE = 0x00000040;
    public static final int BIF_BROWSEINCLUDEURLS = 0x00000080;
    public static final int BIF_USENEWUI = BIF_EDITBOX | BIF_NEWDIALOGSTYLE;
    public static final int BIF_UAHINT = 0x00000100;
    public static final int BIF_NONEWFOLDERBUTTON = 0x00000200;
    public static final int BIF_NOTRANSLATETARGETS = 0x00000400;
    public static final int BIF_BROWSEFORCOMPUTER = 0x00001000;
    public static final int BIF_BROWSEFORPRINTER = 0x00002000;
    public static final int BIF_BROWSEINCLUDEFILES = 0x00004000;
    public static final int BIF_SHAREABLE = 0x00008000;
    public static final int BIF_BROWSEFILEJUNCTIONS = 0x00010000;

    public static native Pointer SHBrowseForFolderW(BrowseInfo lpbi);

    public static native boolean SHGetPathFromIDListW(Pointer pidl, Pointer pszPath);

    static {
        Native.register("shell32");
    }
}
