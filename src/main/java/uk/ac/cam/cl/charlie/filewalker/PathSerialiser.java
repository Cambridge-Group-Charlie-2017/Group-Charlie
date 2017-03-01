package uk.ac.cam.cl.charlie.filewalker;

import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.db.Serializers;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by shyam on 21/02/2017.
 */
public class PathSerialiser extends Serializer<Path> {
    @Override
    public boolean typecheck(Object obj) {
        return obj instanceof Path;
    }

    @Override
    public byte[] serialize(Path object) {
        return Serializers.STRING.serialize(object.toAbsolutePath().toString()); // think this works!
    }

    @Override
    public Path deserialize(byte[] bytes) {
        return Paths.get(Serializers.STRING.deserialize(bytes));
    }
}
