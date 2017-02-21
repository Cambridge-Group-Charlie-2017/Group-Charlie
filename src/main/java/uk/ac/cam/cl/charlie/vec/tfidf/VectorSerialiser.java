package uk.ac.cam.cl.charlie.vec.tfidf;

import uk.ac.cam.cl.charlie.db.Serializer;
import uk.ac.cam.cl.charlie.math.Vector;

import java.nio.ByteBuffer;

/**
 * Created by shyam on 21/02/2017.
 */
public class VectorSerialiser extends Serializer<Vector> {

    private static double[] toDoubleArray(byte[] bytes) {
        // needed for deserialise (inner classes can't have static methods)
        if (bytes.length % 8 != 0) {
            throw new IllegalArgumentException();
        }

        ByteBuffer buf = ByteBuffer.wrap(bytes);

        double[] a = new double[bytes.length / 8];

        for (int i = 0; i < a.length; ++i) {
            a[i] = buf.getDouble();
        }
        return a;
    }
    @Override
    public boolean typecheck(Object obj) {
        return obj instanceof Vector;
    }

    @Override
    public byte[] serialize(Vector object) {
        // there might be a quicker way to do this...
        ByteBuffer buf = ByteBuffer.allocate(object.size() * 8);
        double[] components = object.toDoubleArray();

        for (int i = 0; i < object.size(); ++i) {
            buf.putDouble(components[i]);
        }
        return buf.array();
    }

    @Override
    public Vector deserialize(byte[] bytes) {
        return new Vector(toDoubleArray(bytes));
    }
}
