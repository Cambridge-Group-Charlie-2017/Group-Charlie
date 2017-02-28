package uk.ac.cam.cl.charlie.ClusterStorage;
import org.iq80.leveldb.DB;

import static org.iq80.leveldb.impl.Iq80DBFactory.asString;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;

import uk.ac.cam.cl.charlie.util.OS;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 * Created by Ben on 27/02/2017.
 */

//maintain database for holding table of MessageNumbers to cluster names.
public class PersistantIDStore {
    DB db; //key = msgnumber, val = clustername

    public PersistantIDStore() {
        String path = OS.getAppDataDirectory("AutoArchive");
        String dbpath = path + File.separator + "ClusterStore.db";
        Options options = new Options();
        options.createIfMissing(true);

        try {
            db = factory.open(new File(dbpath), options);
        } catch (IOException e) {
            throw new Error(e);
        }
    }

    public void insert(int num, String name) {
        db.put(bytes(num), bytes(name));
    }
    public void batchInsert(int[] nums, String[] names) throws IOException{
        WriteBatch batch = db.createWriteBatch();
        try {
            int iterations = Math.min(nums.length, names.length);
            for (int i = 0; i < iterations; i++) {
                batch.put(bytes(nums[i]), bytes(names[i]));
            }
            db.write(batch);
        } finally {
            //close the batch to avoid resource leaks.
            batch.close();
        }
    }

    public void delete(int num) {
        db.delete(bytes(num));
    }
    public void batchDelete(int[] nums) throws IOException{
        WriteBatch batch = db.createWriteBatch();
        try {
            for (int n : nums) {
                batch.delete(bytes(n));
            }
            db.write(batch);
        } finally {
            //close the batch to avoid resource leaks.
            batch.close();
        }
    }

    public void move(int num, String name) {
        delete(num);
        insert(num, name);
    }
    public void batchMove(int[] nums, String[] names) throws IOException{
        batchDelete(nums);
        batchInsert(nums, names);
    }

    public int[] getAllWithvalue(String s) throws IOException{
        DBIterator iterator = db.iterator();
        ArrayList<Integer> keysWithValue = new ArrayList<>();

        try {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                Entry<byte[],byte[]> next = iterator.peekNext();
                if (s.equals(new String(next.getValue()))) {
                    keysWithValue.add(ByteBuffer.wrap(next.getKey()).getInt());
                }
            }
        } finally {
            // Make sure you close the iterator to avoid resource leaks.
            iterator.close();
        }
        int[] keys = new int[keysWithValue.size()];
        for (int i = 0; i < keysWithValue.size(); i++) {
            keys[i] = keysWithValue.get(i);
        }
        return keys;
    }

    public void wipeDatabase() throws IOException{
        DBIterator iterator = db.iterator();
        WriteBatch batch = db.createWriteBatch();
        try {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                Entry<byte[],byte[]> next = iterator.peekNext();
                batch.delete(next.getKey());
            }
            db.write(batch);
        } finally {
            // Make sure you close the iterator & batch to avoid resource leaks.
            iterator.close();
            batch.close();
        }
    }

    private byte[] bytes(int n) {
        return ByteBuffer.allocate(4).putInt(n).array();
    }
    private byte[] bytes(String s) {
        return s.getBytes();
    }
}
