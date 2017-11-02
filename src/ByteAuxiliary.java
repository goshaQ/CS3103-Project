import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.UUID;

public class ByteAuxiliary {
    public static byte[] toByteArray(int value) {
        return ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
    }

    public static byte[] toByteArray(String value) {
        byte[] message = null;

        try {
            message = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("Can't encode provided string.");
        }

        return message;
    }

    public static byte[] toByteArray(UUID value) {
        ByteBuffer buffer = ByteBuffer.allocate(16);

        buffer.putLong(value.getMostSignificantBits());
        buffer.putLong(value.getLeastSignificantBits());

        return buffer.array();
    }

    public static String recoverString(byte[] message) {
        String result = null;

        try {
            result = new String(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("Can't display list of available files.");
        }

        return result;
    }

    public static UUID recoverUUID(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);

        long firstLong = buffer.getLong();
        long secondLong = buffer.getLong();

        return new UUID(firstLong, secondLong);
    }
}
