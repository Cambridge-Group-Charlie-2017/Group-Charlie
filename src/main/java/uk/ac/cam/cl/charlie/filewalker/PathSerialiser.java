package uk.ac.cam.cl.charlie.filewalker;

import java.nio.file.Path;
import java.nio.file.Paths;

import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.db.Serializers;

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
        return Serializers.STRING.serialize(object.toAbsolutePath().toString());
    }

    @Override
    public Path deserialize(byte[] bytes) {
        return Paths.get(Serializers.STRING.deserialize(bytes));
    }
}
