package uk.ac.cam.cl.charlie.db;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Serializers for common types
 *
 * @author Gary Guo
 */
public final class Serializers {

    private Serializers() {

    }

    public static final Serializer<String> STRING = new Serializer<String>() {

	@Override
	public boolean typecheck(Object obj) {
	    return obj instanceof String;
	}

	@Override
	public byte[] serialize(String object) {
	    try {
		return object.getBytes("UTF-8");
	    } catch (UnsupportedEncodingException e) {
		throw new AssertionError("UTF-8 should always be supported", e);
	    }
	}

	@Override
	public String deserialize(byte[] bytes) {
	    try {
		return new String(bytes, "UTF-8");
	    } catch (UnsupportedEncodingException e) {
		throw new AssertionError("UTF-8 should always be supported", e);
	    }
	}

    };

    public static final Serializer<Integer> INTEGER = new Serializer<Integer>() {

	@Override
	public boolean typecheck(Object obj) {
	    return obj instanceof Integer;
	}

	@Override
	public byte[] serialize(Integer object) {
	    return ByteBuffer.allocate(4).putInt(object).array();
	}

	@Override
	public Integer deserialize(byte[] bytes) {
	    if (bytes.length != 4) {
		throw new IllegalArgumentException("number of bytes must be 4");
	    }
	    return ByteBuffer.wrap(bytes).getInt();
	}

    };
}
