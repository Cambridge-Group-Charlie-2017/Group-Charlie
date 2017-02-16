package uk.ac.cam.cl.charlie.db;

/**
 * Base class for all serializers. By default all serializer with the same type
 * are considered equal.
 *
 * @author Gary Guo
 */
public abstract class Serializer<T> {

    public abstract boolean typecheck(Object obj);

    public abstract byte[] serialize(T object);

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
