package uk.ac.cam.cl.charlie.db;

import java.io.IOException;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.WriteBatch;

public class PersistentMap<K, V> implements Map<K, V> {

    public class BatchWriter implements AutoCloseable {
        WriteBatch batch = db.createWriteBatch();

        public void put(K key, V value) {
            byte[] keyBytes = keySerializer.serialize(key);
            byte[] valueBytes = valueSerializer.serialize(value);
            batch.put(keyBytes, valueBytes);
        }

        public void remove(K key) {
            byte[] keyBytes = keySerializer.serialize(key);
            batch.delete(keyBytes);
        }

        public void commit() {
            db.write(batch);
        }

        @Override
        public void close() {
            commit();
        }
    }

    class KeySet extends AbstractSet<K> {

        @Override
        public int size() {
            return PersistentMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return PersistentMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return PersistentMap.this.containsKey(o);
        }

        @Override
        public Iterator<K> iterator() {
            DBIterator iter = db.iterator();
            iter.seekToFirst();

            return new Iterator<K>() {

                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public K next() {
                    return keySerializer.deserialize(iter.next().getKey());
                }
            };
        }

        @Override
        public boolean remove(Object o) {
            if (contains(o)) {
                PersistentMap.this.remove(o);
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            PersistentMap.this.clear();
        }

    }

    class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public int size() {
            return PersistentMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return PersistentMap.this.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            if (o instanceof Entry) {
                Entry<?, ?> entry = (Entry<?, ?>) o;
                if (!containsKey(entry.getKey())) {
                    return false;
                }
                return Objects.equals(entry.getValue(), get(entry.getKey()));
            }
            return false;
        }

        @Override
        public Iterator<Entry<K, V>> iterator() {
            DBIterator iter = db.iterator();
            iter.seekToFirst();

            return new Iterator<Entry<K, V>>() {

                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public Entry<K, V> next() {
                    Entry<byte[], byte[]> entry = iter.next();
                    return new Entry<K, V>() {

                        K key;
                        V value;
                        boolean keyLoaded = false;
                        boolean valueLoaded = false;

                        @Override
                        public K getKey() {
                            if (!keyLoaded) {
                                key = keySerializer.deserialize(entry.getKey());
                                keyLoaded = true;
                            }
                            return key;
                        }

                        @Override
                        public V getValue() {
                            if (!valueLoaded) {
                                value = valueSerializer.deserialize(entry.getValue());
                                valueLoaded = true;
                            }
                            return value;
                        }

                        @Override
                        public V setValue(V value) {
                            throw new UnsupportedOperationException();
                        }

                    };
                }
            };
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof Entry) {
                Entry<?, ?> entry = (Entry<?, ?>) o;
                if (!containsKey(entry.getKey())) {
                    return false;
                }
                if (Objects.equals(entry.getValue(), get(entry.getKey()))) {
                    PersistentMap.this.remove(entry.getKey());
                    return true;
                }
            }
            return false;
        }

        @Override
        public void clear() {
            PersistentMap.this.clear();
        }

    }

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
        for (iter.seekToFirst(); iter.hasNext();) {
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
        for (iter.seekToFirst(); iter.hasNext();) {
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
        for (iter.seekToFirst(); iter.hasNext();) {
            Entry<byte[], byte[]> entry = iter.next();
            db.delete(entry.getKey());
        }
    }

    public BatchWriter batch() {
        return new BatchWriter();
    }

    @Override
    public Set<K> keySet() {
        return new KeySet();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    public DB getLevelDB() {
        return this.db;
    }

}
