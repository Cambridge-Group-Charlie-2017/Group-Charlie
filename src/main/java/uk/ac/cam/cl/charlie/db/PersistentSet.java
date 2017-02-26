package uk.ac.cam.cl.charlie.db;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;

/**
 * Created by shyam on 26/02/2017.
 * just a persistent map based set (similar to hashmap and hashset)
 */
public class PersistentSet<T> implements Set<T> {
    private PersistentMap<T, Integer> set;
    private Serializer<T> keySerializer;

    public PersistentSet (Serializer<T> keySerializer, String mapName) {
        set = Database.getInstance().getMap(mapName, keySerializer, Serializers.INTEGER);
        this.keySerializer = keySerializer;
    }

    @Override
    public Spliterator<T> spliterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        return set.size();
    }

    @Override
    public boolean isEmpty() {
        return set.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return set.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return set.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return set.keySet().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return set.keySet().toArray(a);
    }

    @Override
    public boolean add(T t) {
        set.put(t, new Integer(0));
        return true;
    }

    @Override
    public boolean remove(Object o) {
        set.remove(o);
        return true;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return set.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        for (T t : c) {
            set.put(t, new Integer(0));
        }
        return true;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
        // this would take too long and isn't needed.
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        for (Object o : c) {
            set.remove(o);
        }
        return true;
    }

    @Override
    public void clear() {
        set.clear();
    }
}
