package uk.ac.cam.cl.charlie.ClusterStorage;

import javax.mail.*;
import java.util.ArrayList;
import java.util.Collections;

/*
 * @authors: Matt Boyce, Ben O'Neill
 */
public class VirtualFolder extends Folder {
    /**
     * Constructor that takes a Store object.
     *
     * @param store the Store that holds this folder
     */
    private ArrayList<Message> messages = new ArrayList<Message>();
    private String folderName;
    private ArrayList<VirtualFolder> subfolders = new ArrayList<>();
    private Folder parent;

    protected VirtualFolder(Store store, Folder parent) {
        super(store);
        this.parent = parent;
    }

    private static VirtualFolder root;
    private VirtualFolder(Store store) { //root folder
        super(store);
    }
    public static VirtualFolder getRoot(Store store) { //singleton pattern for root folder.
        if (root == null) {
            root = new VirtualFolder(store);
        }
        return root;
    }

    @Override
    public String getName() {
        return folderName;
    }

    @Override
    public String getFullName() {
        return parent.getFullName() + '.' + folderName;
    }

    @Override
    public Folder getParent() throws MessagingException {
        return parent;
    }

    @Override
    public boolean exists() throws MessagingException {
        if(store.getFolder(this.folderName) != null)
            return true;
        return false;
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        //not really appliable. Only single level folders.
        return null;
    }

    @Override
    public char getSeparator() throws MessagingException {
        return '.';
    }

    @Override
    public int getType() throws MessagingException {
        return HOLDS_MESSAGES;
    }

    @Override
    public boolean create(int type) throws MessagingException {
        //Method shouldn't be used. Store only contains representations of clusters.
        return false;
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        //TODO: Possibly could return true if new messages inserted?
        return false;
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        for (VirtualFolder folder : subfolders)
            if (folder.getName().equals(name))
                return folder;
        return null;
    }

    @Override
    public boolean delete(boolean recurse) throws MessagingException {
        //No actions need be taken. Irrelevant method.
        return true;
    }

    @Override
    public boolean renameTo(Folder f) throws MessagingException {
        //should only be applied to a closed folder. This folder never closed during runtime, so not applicable.
        return false;
    }

    @Override
    public void open(int mode) throws MessagingException {
        //Not relevant
    }

    @Override
    public void close(boolean expunge) throws MessagingException {
        //Should be sufficient to just drop reference and let folder be garbage collected.
    }

    @Override
    public boolean isOpen() {
        //folder is always open
        return true;
    }

    @Override
    //Flags not relevant.
    public Flags getPermanentFlags() {
        return null;
    }

    @Override
    public int getMessageCount() throws MessagingException {
        return messages.size();
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        //return a message with a given message number
        for (Message msg : messages)
            if (msg.getMessageNumber() == msgnum)
                return msg;
        return null;
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        //Adds messages to folder.
        Collections.addAll(messages, msgs);
    }

    public void removeMessage(Message msg) {
        messages.remove(msg);
    }

    @Override
    public Message[] expunge() throws MessagingException {
        //remove all messages marked DELETED. This is automatically done by ClusteringMailStore.
        return new Message[0];
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
}
