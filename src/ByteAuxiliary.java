import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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

    public static byte[] toByteArray(InetAddress value) {
        return value.getAddress();
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
        int length = FileInfo.BYTES + (value.fileName.length() * Byte.BYTES) + (value.pieceHashes.length * FileInfo.SHA_1.getDigestLength()) + 1;
        byte[] buffer = new byte[length];

        int offset = 0;
        // Write the name of file
        System.arraycopy(toByteArray(value.fileName + '\n'), 0, buffer, offset, value.fileName.length() + 1);
        // Write the size of file
        System.arraycopy(toByteArray(value.size), 0, buffer, (offset += value.fileName.length() + 1), Long.BYTES);
        // Write the size of piece
        System.arraycopy(toByteArray(value.pieceSize), 0, buffer, (offset += Long.BYTES), Integer.BYTES);
        // Write the number of pieces
        System.arraycopy(toByteArray(value.pieceCount), 0, buffer, (offset += Integer.BYTES), Integer.BYTES);
        // Write the hash for each piece
        offset += Integer.BYTES;
        for (byte[] pieceHash : value.pieceHashes) {
            System.arraycopy(pieceHash, 0, buffer, offset, FileInfo.SHA_1.getDigestLength());
            offset += FileInfo.SHA_1.getDigestLength();
        }

        return buffer;
    }

    public static byte[] toByteArray(PeerInfo value) {
        int length = PeerInfo.BYTES;
        byte[] buffer = new byte[length];

        int offset = 0;
        // Write the peer ID
        System.arraycopy(toByteArray(value.peerID), 0, buffer, offset, (2 * Long.BYTES));
        // Write inet address of the peer
        System.arraycopy(value.inetAddress.getAddress(), 0, buffer, (offset += (2 * Long.BYTES)), Integer.BYTES);
        // Write port of the peer
        System.arraycopy(toByteArray(value.port), 0, buffer, (offset += Integer.BYTES), Integer.BYTES);

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

    public static InetAddress recoverInetAddress(byte[] message) {
        InetAddress tmp = null;
        try {
            tmp = InetAddress.getByAddress(message);
        } catch (UnknownHostException e) {
            System.err.println("There is no such inet address!");
        }

        return tmp;
    }

    public static UUID recoverUUID(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message);

        long firstLong = buffer.getLong();
        long secondLong = buffer.getLong();

        return new UUID(firstLong, secondLong);
    }

    public static FileInfo recoverFileInfo(byte[] message) {
        int fileNameLength = 0, offset = 0;

        // Find the length of the file name
        for (byte character : message) {
            if (character == '\n') {
                break;
            }

            fileNameLength++;
        }

        // Recover the name of file
        String fileName = recoverString(Arrays.copyOfRange(message, offset, (offset += fileNameLength)));
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

    public static PeerInfo recoverPeerInfo(byte[] message) {
        int offset = 0;

        // Recover the peer ID
        UUID peerID = recoverUUID(Arrays.copyOfRange(message, offset, (offset += (2 * Long.BYTES))));
        // Recover inet address of the peer
        InetAddress inetAddress = ByteAuxiliary.recoverInetAddress(Arrays.copyOfRange(message, offset, (offset += Integer.BYTES)));
        // Recover port of the peer
        int port = recoverInt(Arrays.copyOfRange(message, offset, (offset += Integer.BYTES)));

        return new PeerInfo(peerID, inetAddress, port);
    }
}
