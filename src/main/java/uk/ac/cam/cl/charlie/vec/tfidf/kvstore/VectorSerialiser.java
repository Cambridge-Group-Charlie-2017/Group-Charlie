package uk.ac.cam.cl.charlie.vec.tfidf.kvstore;

import java.nio.ByteBuffer;

import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.math.Vector;

/**
 * @author Shyam Tailor
 */
public class VectorSerialiser extends Serializer<Vector> {
    @Override
    public boolean typecheck(Object obj) {
        return obj instanceof Vector;
    }

    @Override
    public byte[] serialize(Vector object) {
        ByteBuffer buf = ByteBuffer.allocate(object.size() * 8);

        for (int i = 0; i < object.size(); ++i) {
            buf.putDouble(object.get(i));
        }
        return buf.array();
    }

    @Override
    public Vector deserialize(byte[] bytes) {
        if (bytes.length % 8 != 0) {
            throw new IllegalArgumentException();
        }

        ByteBuffer buf = ByteBuffer.wrap(bytes);

        double[] array = new double[bytes.length / 8];

        for (int i = 0; i < array.length; ++i) {
            array[i] = buf.getDouble();
        }

        return new Vector(array);
    }
}