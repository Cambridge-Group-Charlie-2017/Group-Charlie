package uk.ac.cam.cl.charlie.db;

import java.io.IOException;
import java.util.*;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

public class PersistentMap<K, V> implements Map<K, V> {

    Serializer<K> keySerializer;
    Serializer<V> valueSerializer;
    DB db;

    public PersistentMap(DB db, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
        this.db = db;
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
    }

    @Override
    public int size() {
        int size = 0;
        DBIterator iter = db.iterator();
        for (iter.seekToFirst(); iter.hasNext(); ) {
            iter.next();
            size++;
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        try (DBIterator iterator = db.iterator()) {
            iterator.seekToFirst();
            if (iterator.hasNext()) {
                return false;
            }
        } catch (IOException e) {
            throw new Error(e);
        }
        return true;
    }

    @Override
    public boolean containsKey(Object key) {
        if (!keySerializer.typecheck(key)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        byte[] keyBytes = keySerializer.serialize((K) key);
        byte[] valueBytes = db.get(keyBytes);
        if (valueBytes != null) {
            return true;
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (!valueSerializer.typecheck(value)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        byte[] valueBytes = valueSerializer.serialize((V) value);
        DBIterator iter = db.iterator();
        for (iter.seekToFirst(); iter.hasNext(); ) {
            Entry<byte[], byte[]> entry = iter.next();
            if (Arrays.equals(valueBytes, entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        if (!keySerializer.typecheck(key)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        byte[] keyBytes = keySerializer.serialize((K) key);
        byte[] valueBytes = db.get(keyBytes);
        if (valueBytes != null) {
            return valueSerializer.deserialize(valueBytes);
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        byte[] keyBytes = keySerializer.serialize(key);
        byte[] valueBytes = valueSerializer.serialize(value);
        byte[] oldValueBytes = db.get(keyBytes);
        db.put(keyBytes, valueBytes);
        if (oldValueBytes != null) {
            return valueSerializer.deserialize(oldValueBytes);
        }
        return null;
    }

    @Override
    public V remove(Object key) {
        if (!keySerializer.typecheck(key)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        byte[] keyBytes = keySerializer.serialize((K) key);
        byte[] valueBytes = db.get(keyBytes);
        db.delete(keyBytes);
        if (valueBytes != null) {
            return valueSerializer.deserialize(valueBytes);
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        DBIterator iter = db.iterator();
        for (iter.seekToFirst(); iter.hasNext(); ) {
            Entry<byte[], byte[]> entry = iter.next();
            db.delete(entry.getKey());
        }
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        DBIterator it = db.iterator();
        it.seekToFirst();
        Set<Map.Entry<K,V>> entries = new HashSet<>();
        while (it.hasNext()) {
            Map.Entry<byte[], byte[]> rawEntry = it.next();
            K key = keySerializer.deserialize(rawEntry.getKey());
            V val = valueSerializer.deserialize(rawEntry.getValue());
            Map.Entry<K,V> entry = new AbstractMap.SimpleEntry<K,V>(key, val);
            entries.add(entry);
        }
        return entries;
    }

}
