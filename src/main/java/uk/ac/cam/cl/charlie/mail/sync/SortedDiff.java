package uk.ac.cam.cl.charlie.mail.sync;

import java.util.Iterator;
import java.util.Map.Entry;

import uk.ac.cam.cl.charlie.util.PeekableIterator;

public abstract class SortedDiff<K extends Comparable<K>, V1, V2> {

    @SuppressWarnings("serial")
    public static class BreakException extends RuntimeException {
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }
    }

    public SortedDiff() {

    }

    protected void breakExecution() {
        throw new BreakException();
    }

    public void diff(Iterator<Entry<K, V1>> oldIter, Iterator<Entry<K, V2>> newIter) {
        try {
            PeekableIterator<Entry<K, V2>> newPeekIter = new PeekableIterator<>(newIter);
            while (oldIter.hasNext()) {
                Entry<K, V1> entry = oldIter.next();
                K key = entry.getKey();

                while (true) {
                    if (!newIter.hasNext()) {
                        onRemove(entry);
                        break;
                    } else {
                        int result = key.compareTo(newPeekIter.peek().getKey());
                        if (result == 0) {
                            // Same key, that's what we like
                            onNoChange(entry, newPeekIter.next());
                            break;
                        } else if (result < 0) {
                            onRemove(entry);
                            break;
                        } else {
                            onAdd(newPeekIter.next());
                            continue;
                        }
                    }
                }
            }

            while (newPeekIter.hasNext()) {
                onAdd(newPeekIter.next());
            }

        } catch (BreakException e) {

        }
    }

    protected abstract void onRemove(Entry<K, V1> entry);

    protected void onNoChange(Entry<K, V1> e1, Entry<K, V2> e2) {

    }

    protected abstract void onAdd(Entry<K, V2> entry);

}
