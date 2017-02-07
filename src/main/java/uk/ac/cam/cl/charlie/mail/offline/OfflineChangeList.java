package uk.ac.cam.cl.charlie.mail.offline;

import java.util.Stack;

/**
 * Created by Simon on 07/02/2017.
 */
public class OfflineChangeList {
    public final static OfflineChangeList instance = new OfflineChangeList();


    Stack<OfflineChange> changes;

    private OfflineChangeList() {
        changes = new Stack<>();
    }

    public void performChanges() {
        while (!changes.isEmpty()) {
            changes.pop().handleChange();
        }
    }

    public void addChange(OfflineChange change) {
        changes.push(change);
    }
}
