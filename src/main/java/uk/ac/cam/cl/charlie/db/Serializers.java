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

    public static final Serializer<Boolean> BOOLEAN = new Serializer<Boolean>() {

        @Override
        public boolean typecheck(Object obj) {
            return obj instanceof Boolean;
        }

        @Override
        public byte[] serialize(Boolean object) {
            return ByteBuffer.allocate(1).put(object.booleanValue() ? (byte) 1 : (byte) 0).array();
        }

        @Override
        public Boolean deserialize(byte[] bytes) {
            if (bytes.length != 1) {
                throw new IllegalArgumentException("number of bytes must be 1");
            }
            return ByteBuffer.wrap(bytes).get() != 0;
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

    public static final Serializer<Long> LONG = new Serializer<Long>() {

        @Override
        public boolean typecheck(Object obj) {
            return obj instanceof Long;
        }

        @Override
        public byte[] serialize(Long object) {
            return ByteBuffer.allocate(8).putLong(object).array();
        }

        @Override
        public Long deserialize(byte[] bytes) {
            if (bytes.length != 8) {
                throw new IllegalArgumentException("number of bytes must be 8");
            }
            return ByteBuffer.wrap(bytes).getLong();
        }

    };

    public static final Serializer<byte[]> BYTE_ARRAY = new Serializer<byte[]>() {
        @Override
        public boolean typecheck(Object obj) {
            return obj instanceof byte[];
        }

        @Override
        public byte[] serialize(byte[] object) {
            return object;
        }

        @Override
        public byte[] deserialize(byte[] bytes) {
            return bytes;
        }
    };
}
