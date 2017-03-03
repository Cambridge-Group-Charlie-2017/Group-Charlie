package uk.ac.cam.cl.charlie.clustering.store;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Store;

import uk.ac.cam.cl.charlie.clustering.clusters.Cluster;

/**
 * @author Matt Boyce
 * @author Ben O'Neill
 * @author Gary Guo
 */
public class VirtualFolder extends Folder {

    private Cluster<Message> cluster;
    private Folder parent;

    protected VirtualFolder(Store store, Folder parent, Cluster<Message> cluster) {
        super(store);

        this.parent = parent;
        this.cluster = cluster;
    }

    private VirtualFolder(Store store) { // root folder
        super(store);
    }

    @Override
    public String getName() {
        return cluster.getName();
    }

    @Override
    public String getFullName() {
        return parent.getFullName() + '/' + getName();
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
        return cluster.getSize();
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        // return a message with a given message number
        return cluster.getObjects().get(msgnum - 1).getObject();
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Message[] expunge() throws MessagingException {
        throw new UnsupportedOperationException();
    }

}
