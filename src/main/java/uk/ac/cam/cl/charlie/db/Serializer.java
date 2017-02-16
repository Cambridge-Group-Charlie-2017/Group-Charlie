package uk.ac.cam.cl.charlie.db;

/**
 * Base class for all serializers. By default all serializer with the same type
 * are considered equal.
 *
 * @author Gary Guo
 */
public abstract class Serializer<T> {

    /**
     * Check whether the object is serializable by this serializer. This check
     * is necessary since Java do type erasure instead of true generic. This
     * should return obj instanceof T.
     *
     * @param obj
     *            the object to check
     * @return {@code true} if the object is of type T, and {@code false} if it
     *         isn't.
     */
    public abstract boolean typecheck(Object obj);

    /**
     * Serialize object to byte array. If two objects compared equal, they
     * should produce the same serialized result.
     *
     * @param object
     *            the object to serialize
     * @return serialized byte array
     */
    public abstract byte[] serialize(T object);

    /**
     * De-serialize byte array to recover the object. Objects deserialized from
     * the same sequence of bytes should compare equal.
     *
     * @param bytes
     *            byte array to de-serialize
     * @return deserialized object
     */
    public abstract T deserialize(byte[] bytes);

    @Override
    public boolean equals(Object obj) {
	return obj.getClass() == getClass();
    }

    @Override
    public int hashCode() {
	return getClass().hashCode();
    }

}
