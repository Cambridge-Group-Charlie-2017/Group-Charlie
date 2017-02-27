package uk.ac.cam.cl.charlie.ClusterStorage;

import javax.mail.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Created by M Boyce on 27/02/2017.
 */
public class VirtualFolder extends Folder {
    /**
     * Constructor that takes a Store object.
     *
     * @param store the Store that holds this folder
     */
    private ArrayList<Message> messages = new ArrayList<Message>();
    private String folderName;

    protected VirtualFolder(Store store) {
        super(store);
    }

    @Override
    public String getName() {
        return folderName;
    }

    @Override
    public String getFullName() {
        return null;
    }

    @Override
    public Folder getParent() throws MessagingException {
        return null;
    }

    @Override
    public boolean exists() throws MessagingException {
        return true;
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        return new Folder[0];
    }

    @Override
    public char getSeparator() throws MessagingException {
        return 0;
    }

    @Override
    public int getType() throws MessagingException {
        return HOLDS_MESSAGES;
    }

    @Override
    public boolean create(int type) throws MessagingException {
        return false;
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        return false;
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        return null;
    }

    @Override
    public boolean delete(boolean recurse) throws MessagingException {
        return false;
    }

    @Override
    public boolean renameTo(Folder f) throws MessagingException {
        return false;
    }

    @Override
    public void open(int mode) throws MessagingException {

    }

    @Override
    public void close(boolean expunge) throws MessagingException {

    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public Flags getPermanentFlags() {
        return null;
    }

    @Override
    public int getMessageCount() throws MessagingException {
        return 0;
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        return null;
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        Collections.addAll(messages, msgs);
    }

    @Override
    public Message[] expunge() throws MessagingException {
        return new Message[0];
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }
}
