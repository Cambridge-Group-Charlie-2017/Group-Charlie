package uk.ac.cam.cl.charlie.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Deferred<V> implements Future<V> {

    boolean completed;
    Throwable exception;
    V value;
    private Object lock = new Object();

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException("cancel not supported");
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        synchronized (lock) {
            return completed;
        }
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        synchronized (lock) {
            if (!completed) {
                lock.wait();
            }
            if (exception != null) {
                throw new ExecutionException(exception);
            }
            return value;
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        synchronized (lock) {
            if (!completed) {
                lock.wait(unit.toMillis(timeout));
            }
            if (!completed)
                throw new TimeoutException();
            if (exception != null) {
                throw new ExecutionException(exception);
            }
            return value;
        }
    }

    public void throwException(Throwable t) {
        synchronized (lock) {
            if (completed)
                throw new IllegalStateException("Deferred resolved already");
            completed = true;
            exception = t;
            lock.notifyAll();
        }
    }

    public void setValue(V v) {
        synchronized (lock) {
            if (completed)
                throw new IllegalStateException("Deferred resolved already");
            completed = true;
            value = v;
            lock.notifyAll();
        }
    }

}
