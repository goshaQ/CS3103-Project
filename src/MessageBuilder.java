import java.util.BitSet;
import java.util.UUID;

public class MessageBuilder {
    public static byte[] buildHandshakeMessage(byte[] hash, UUID peerID) {
        int offset = 0, length = 21 + hash.length;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, offset, Integer.BYTES);
        // Write the type of message
        message[4] = (byte) MessageType.Handshake.getValue();
        // Write the file info hash
        System.arraycopy(hash, 0, message, (offset += Integer.BYTES + 1), hash.length);
        // Write the peerID
        System.arraycopy(ByteAuxiliary.toByteArray(peerID), 0, message, (offset += hash.length), 2 * Long.BYTES);

        return message;
    }

    public static byte[] buildAvailablePiecesMessage(BitSet pieces) {
        int offset = 0, piecesSize = (int) Math.ceil((double) pieces.length() / Byte.SIZE), length = 5 + piecesSize;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, offset, Integer.BYTES);
        // Write the type of message
        message[4] = (byte) MessageType.AvailablePieces.getValue();
        // Write available pieces of the client
        System.arraycopy(ByteAuxiliary.toByteArray(pieces), 0, message, (offset += Integer.BYTES + 1), piecesSize);

        return message;
    }

    public static byte[] buildDataRequestMessage(short pieceIndex) {
        int offset = 0, length = 7;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, offset, Integer.BYTES);
        // Write the type of message
        message[4] = (byte) MessageType.DataRequest.getValue();
        // Write the index of requesting piece
        System.arraycopy(ByteAuxiliary.toByteArray(pieceIndex), 0, message, (offset += Integer.BYTES + 1), Short.BYTES);

        return message;
    }

    public static byte[] buildDataPackageMessage(short pieceIndex, byte[] data) {
        int offset = 0, length = 7 + data.length;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, offset, Integer.BYTES);
        // Write the type of message
        message[4] = (byte) MessageType.DataPackage.getValue();
        // Write the index of providing piece
        System.arraycopy(ByteAuxiliary.toByteArray(pieceIndex), 0, message, (offset += Integer.BYTES + 1), Short.BYTES);
        // Write the piece
        System.arraycopy(data, 0, message, (offset += Short.BYTES), data.length);

        return message;
    }

    public static byte[] buildPieceUpdateMessage(short pieceIndex) {
        int offset = 0, length = 7;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, offset, Integer.BYTES);
        // Write the type of message
        message[4] = (byte) MessageType.PieceUpdate.getValue();
        // Write the index of obtained piece
        System.arraycopy(ByteAuxiliary.toByteArray(pieceIndex), 0, message, (offset += Integer.BYTES + 1), Short.BYTES);

        return message;
    }

    public static byte[] buildDirectoryListingRequestMessage() {
        int length = 5;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, 0, Integer.BYTES);
        // Write the type of message
        message[4] = (byte) MessageType.DirectoryListingRequest.getValue();

        return message;
    }

    public static byte[] buildDirectoryListingReplyMessage(String fileNames) {
        int offset = 0, length = 5 + fileNames.length();

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, offset, Integer.BYTES);
        // Write the type of message
        message[4] = (byte) MessageType.DirectoryListingReply.getValue();
        // Write the provided file names
        System.arraycopy(ByteAuxiliary.toByteArray(fileNames), 0, message, (offset += Integer.BYTES + 1), fileNames.length());

        return message;
    }
}
