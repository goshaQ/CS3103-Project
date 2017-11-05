import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;

public class ByteAuxiliary {
    public static byte[] toByteArray(short value) {
        return ByteBuffer.allocate(Short.BYTES).order(ByteOrder.BIG_ENDIAN).putShort(value).array();
    }

    public static byte[] toByteArray(int value) {
        return ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN).putInt(value).array();
    }

    public static byte[] toByteArray(long value) {
        return ByteBuffer.allocate(Long.BYTES).order(ByteOrder.BIG_ENDIAN).putLong(value).array();
    }

    public static byte[] toByteArray(String value) {
        byte[] buffer = null;

        try {
            buffer = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("Can't encode provided string.");
        }

        return buffer;
    }

    public static byte[] toByteArray(UUID value) {
        ByteBuffer buffer = ByteBuffer.allocate(2 * Long.BYTES);

        buffer.putLong(value.getMostSignificantBits());
        buffer.putLong(value.getLeastSignificantBits());

        return buffer.array();
    }

    public static byte[] toByteArray(BitSet value) {
        return value.toByteArray();
    }

    public static byte[] toByteArray(FileInfo value) {
        int bufferLength = value.fileName.length() + 1 + (Long.BYTES) +(2 * Integer.BYTES) + (value.pieceHashes.length * FileInfo.SHA_1.getDigestLength());
        byte[] buffer = new byte[bufferLength];

        int offset = 0;
        // Write the name of file
        System.arraycopy(ByteAuxiliary.toByteArray(value.fileName + '\n'), 0, buffer, offset, value.fileName.length());
        // Write the size of file
        System.arraycopy(ByteAuxiliary.toByteArray(value.size), 0, buffer, (offset += value.fileName.length() + 1), Long.BYTES);
        // Write the size of piece
        System.arraycopy(ByteAuxiliary.toByteArray(value.pieceSize), 0, buffer, (offset += Long.BYTES), Integer.BYTES);
        // Write the number of pieces
        System.arraycopy(ByteAuxiliary.toByteArray(value.pieceCount), 0, buffer, (offset += Integer.BYTES), Integer.BYTES);
        // Write the hash for each piece
        offset += Integer.BYTES;
        for (byte[] pieceHash : value.pieceHashes) {
            System.arraycopy(pieceHash, 0, buffer, offset, FileInfo.SHA_1.getDigestLength());
            offset += FileInfo.SHA_1.getDigestLength();
        }

        return buffer;
    }

    public static short recoverShort(byte[] message) {
        return ByteBuffer.wrap(message).getShort();
    }

    public static int recoverInt(byte[] message) {
        return ByteBuffer.wrap(message).getInt();
    }

    public static long recoverLong(byte[] message) {
        return ByteBuffer.wrap(message).getLong();
    }

    public static String recoverString(byte[] message) {
        String buffer = null;

        try {
            buffer = new String(message, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            System.err.println("Can't display list of available files.");
        }

        return buffer;
    }

    public static BitSet recoverBitSet(byte[] message) {
        return BitSet.valueOf(message);
    }

    public static UUID recoverUUID(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);

        long firstLong = buffer.getLong();
        long secondLong = buffer.getLong();

        return new UUID(firstLong, secondLong);
    }

    public static FileInfo recoverFileInfo(byte[] message) {
        int offset = 0;

        // Recover the name of file
        String fileName = recoverString(Arrays.copyOfRange(message, offset, (offset += Arrays.binarySearch(message, (byte) '\n'))));
        // Recover the size of file
        long size = recoverLong(Arrays.copyOfRange(message, (offset += 1), (offset += Long.BYTES)));
        // Recover the size of piece
        int pieceSize = recoverInt(Arrays.copyOfRange(message, offset, (offset += Integer.BYTES)));
        // Recover the number of pieces
        int pieceCount = recoverInt(Arrays.copyOfRange(message, offset, (offset += Integer.BYTES)));
        // Recover the hash for each piece
        byte[][] pieceHashes = new byte[pieceCount][];
        for (int i = 0; i < pieceCount; i++) {
            pieceHashes[i] = Arrays.copyOfRange(message, offset, (offset += FileInfo.SHA_1.getDigestLength()));
        }

        return new FileInfo(fileName, size, pieceSize, pieceCount, pieceHashes);
    }
}
