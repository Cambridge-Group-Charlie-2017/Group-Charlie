package uk.ac.cam.cl.charlie.util;

import java.util.Iterator;

public class PeekableIterator<T> implements Iterator<T> {

    private Iterator<T> iter;
    private T buffer;
    private boolean hasBuffer;

    public PeekableIterator(Iterator<T> iter) {
        this.iter = iter;
    }

    @Override
    public boolean hasNext() {
        if (hasBuffer)
            return true;
        return iter.hasNext();
    }

    @Override
    public T next() {
        if (hasBuffer) {
            T ret = buffer;
            buffer = null;
            hasBuffer = false;
            return ret;
        }
        return iter.next();
    }

    public T peek() {
        if (hasBuffer) {
            return buffer;
        }
        buffer = iter.next();
        hasBuffer = true;
        return buffer;
    }

}
