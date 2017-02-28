package uk.ac.cam.cl.charlie.clustering.store;

import java.util.ArrayList;
import java.util.Collections;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

/**
 * @author Matt Boyce
 * @author Ben O'Neill
 * @author Gary Guo
 */
public class VirtualFolder extends Folder {

    private ArrayList<Message> messages = new ArrayList<>();
    private String clusterName;
    private Folder parent;

    protected VirtualFolder(Store store, Folder parent, String clusterName) {
        super(store);

        this.parent = parent;
        this.clusterName = clusterName;
    }

    private static VirtualFolder root;

    private VirtualFolder(Store store) { // root folder
        super(store);
    }

    public static VirtualFolder getRoot(Store store) { // singleton pattern for
                                                       // root folder.
        if (root == null) {
            root = new VirtualFolder(store);
        }
        return root;
    }

    @Override
    public String getName() {
        return clusterName;
    }

    @Override
    public String getFullName() {
        return parent.getFullName() + '/' + clusterName;
    }

    @Override
    public Folder getParent() throws MessagingException {
        return parent;
    }

    @Override
    public boolean exists() throws MessagingException {
        return true;
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public char getSeparator() throws MessagingException {
        return '/';
    }

    @Override
    public int getType() throws MessagingException {
        return HOLDS_MESSAGES;
    }

    @Override
    public boolean create(int type) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean delete(boolean recurse) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean renameTo(Folder f) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void open(int mode) throws MessagingException {
        // The folder is always open
    }

    @Override
    public void close(boolean expunge) throws MessagingException {
        // The folder is always open
    }

    @Override
    public boolean isOpen() {
        // The folder is always open
        return true;
    }

    @Override
    public Flags getPermanentFlags() {
        // Flags not relevant.
        return null;
    }

    @Override
    public int getMessageCount() throws MessagingException {
        return messages.size();
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        // return a message with a given message number
        return messages.get(msgnum - 1);
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message[] expunge() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    protected void addMessage(Message msg) {
        messages.add(msg);
    }

    protected void addMessage(Message[] msgs) {
        Collections.addAll(messages, msgs);
    }
}
