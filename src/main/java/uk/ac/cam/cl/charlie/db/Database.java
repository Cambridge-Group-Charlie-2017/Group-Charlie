package uk.ac.cam.cl.charlie.db;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.charlie.util.OS;

/**
 * Class for connecting to the database
 *
 * @author Gary Guo
 */
public class Database {

    private static Database instance;
    private static final Logger log = LoggerFactory.getLogger(Database.class);

    private Map<String, PersistentMap<?, ?>> databases = new HashMap<>();

    private Database() {

    }

    public static void panic(Exception e) {
	log.error("Error when accessing the database", e);
	throw new Error(e);
    }

    /**
     * Get the singleton instance of Database object.
     *
     * @return instance of Database
     */
    public static Database getInstance() {
	if (instance == null) {
	    instance = new Database();
	}
	return instance;
    }

    /**
     * Get a map which is backed by a on-disk LevelDB database. If there is an
     * open connection to the database, it will be reused. If the database does
     * not already exist, it will be created. All connections to the same
     * database must use same pair of serializers.
     *
     * @param name
     *            name of the database
     * @param keySerializer
     *            the serializer used to marshal/unmarshal the key
     * @param valueSerializer
     *            the serializer used to marshal/unmarshal the value
     * @return a map backed by the database
     */
    public <K, V> PersistentMap<K, V> getMap(String name, Serializer<K> keySerializer, Serializer<V> valueSerializer) {
	@SuppressWarnings("unchecked")
	PersistentMap<K, V> map = (PersistentMap<K, V>) databases.get(name);
	if (map != null) {
	    if (!map.keySerializer.equals(keySerializer) || map.valueSerializer.equals(valueSerializer)) {
		throw new RuntimeException("Serializers does not match with existing connections");
	    }
	} else {
	    String path = OS.getAppDataDirectory("AutoArchive");
	    String dbpath = path + File.separator + name + ".db";

	    Options options = new Options();
	    options.createIfMissing(true);
	    DB db;
	    try {
		db = factory.open(new File(dbpath), options);
	    } catch (IOException e) {
		throw new Error(e);
	    }
	    map = new PersistentMap<>(db, keySerializer, valueSerializer);
	    databases.put(name, map);
	}
	return map;
    }

}
