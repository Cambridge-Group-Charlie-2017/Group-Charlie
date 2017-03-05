package uk.ac.cam.cl.charlie.clustering.store;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;

public class UnclusteredFolder extends Folder {

    Folder actual;

    public UnclusteredFolder(ClusteredStore store, Folder actual) {
        super(store);
        this.actual = actual;
    }

    @Override
    public String getName() {
        return actual.getName();
    }

    @Override
    public String getFullName() {
        return actual.getFullName();
    }

    @Override
    public Folder getParent() throws MessagingException {
        // Instead of getting parent directly, we request it from the store
        return store.getFolder(actual.getParent().getFullName());
    }

    @Override
    public boolean exists() throws MessagingException {
        return actual.exists();
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        if (getFullName().isEmpty() && pattern.equals("*")) {
            Folder[] folders = actual.list(pattern);
            List<Folder> p = Arrays.asList(folders).stream().map(f -> {
                try {
                    return store.getFolder(f.getFullName());
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toList());
            ArrayList<Folder> list = new ArrayList<>(p);
            for (Folder f : p) {
                if (f instanceof ClusteredFolder) {
                    Collections.addAll(list, f.list("%"));
                }
            }
            return list.toArray(new Folder[list.size()]);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public char getSeparator() throws MessagingException {
        return actual.getSeparator();
    }

    @Override
    public int getType() throws MessagingException {
        return actual.getType();
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
        actual.open(mode);
    }

    @Override
    public void close(boolean expunge) throws MessagingException {
        actual.close(expunge);
    }

    @Override
    public boolean isOpen() {
        return actual.isOpen();
    }

    @Override
    public Flags getPermanentFlags() {
        return actual.getPermanentFlags();
    }

    @Override
    public int getMessageCount() throws MessagingException {
        return actual.getMessageCount();
    }

    @Override
    public Message getMessage(int msgnum) throws MessagingException {
        return actual.getMessage(msgnum);
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        actual.appendMessages(msgs);
    }

    @Override
    public Message[] expunge() throws MessagingException {
        return actual.expunge();
    }

}
