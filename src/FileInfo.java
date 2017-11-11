import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileInfo {
    public static final int BYTES = Long.BYTES + Integer.BYTES + Integer.BYTES;

    public static final MessageDigest SHA_1;
    static {
        MessageDigest tmp = null;
        try {
            tmp = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            System.err.println("There is no such algorithm for encoding!");
        }

        SHA_1 = tmp;
    }

    public final long size;
    public final int pieceSize;
    public final int pieceCount;

    public final byte[][] pieceHashes;
    public final byte[] hash;

    public final String fileName;

    public FileInfo(String fileName, long size, int pieceSize, int pieceCount, byte[][] pieceHashes) {
        this.fileName = fileName;
        this.size = size;
        this.pieceSize = pieceSize;
        this.pieceCount = pieceCount;

        if (pieceHashes != null) {
            this.pieceHashes = pieceHashes;
            this.hash = getHash();
        } else {
            this.pieceHashes = null;
            this.hash = null;
        }
    }

    private byte[] getHash() {
        byte[] buffer = ByteAuxiliary.toByteArray(this);
        return SHA_1.digest(buffer);
    }
}
