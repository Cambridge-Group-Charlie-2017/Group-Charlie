package uk.ac.cam.cl.charlie.ClusterStorage;

import org.iq80.leveldb.DB;
import static org.iq80.leveldb.impl.Iq80DBFactory.factory;
import org.iq80.leveldb.DBIterator;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;

import uk.ac.cam.cl.charlie.util.OS;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Map.Entry;

/**
 * Created by Ben on 27/02/2017.
 */

//maintain database for holding table of MessageNumbers to cluster names.
public class PersistantIDStore {
    DB db; //key = msgnumber, val = clustername

    /*
     * Initialise database in same directory as the vector database.
     * Get the database instance
     */
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

    //Add single entry
    public void insert(int num, String name) {
        db.put(bytes(num), bytes(name));
    }
    //Add batch (more efficient
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

    //Delete single entry
    public void delete(int num) {
        db.delete(bytes(num));
    }
    //Delete batch (more efficient
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

    //Get a list of all unique values stored in the
    public String[] getVals() throws IOException{
        DBIterator iterator = db.iterator();
        ArrayList<String> valArr = new ArrayList<>();

        try {
            for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
                Entry<byte[],byte[]> next = iterator.peekNext();
                if (!valArr.contains(new String(next.getValue()) ) ) {
                    valArr.add(new String(next.getValue()));
                }
            }
        } finally {
            // Make sure you close the iterator to avoid resource leaks.
            iterator.close();
        }
        String[] vals = new String[valArr.size()];
        for (int i = 0; i < valArr.size(); i++) {
            vals[i] = valArr.get(i);
        }
        return vals;
    }

    //Assigns 'num' a new 'name'
    public void move(int num, String name) {
        delete(num);
        insert(num, name);
    }
    //batch reassign
    public void batchMove(int[] nums, String[] names) throws IOException{
        batchDelete(nums);
        batchInsert(nums, names);
    }

    //Return all keys with supplied value
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

    //Totally wipe database (in preparation for new clustering)
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

    //Need to convert keys and values into byte[] to enter into database
    private byte[] bytes(int n) {
        return ByteBuffer.allocate(4).putInt(n).array();
    }
    private byte[] bytes(String s) {
        return s.getBytes();
    }
}
