import java.util.BitSet;
import java.util.UUID;

public class MessageBuilder {
    public static byte[] buildHandshakeMessage(UUID peerID) {
        int length = 21;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, 0, 4);
        // Write the type of message
        message[4] = (byte) MessageType.Handshake.getValue();
        // Write the peerID
        System.arraycopy(ByteAuxiliary.toByteArray(peerID), 0, message, 5, 16);

        return message;
    }

    public static byte[] buildAvailablePiecesMessage(UUID peerID, BitSet pieces) {
        // ...
        return null;
    }

    public static byte[] buildDirectoryListingRequestMessage() {
        int length = 5;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, 0, 4);
        // Write the type of message
        message[4] = (byte) MessageType.DirectoryListingRequest.getValue();

        return message;
    }

    public static byte[] buildDirectoryListingReplyMessage(String fileNames) {
        int length = 5 + fileNames.length();

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, 0, 4);
        // Write the type of message
        message[4] = (byte) MessageType.DirectoryListingRequest.getValue();
        // Write the provided filenames
        System.arraycopy(ByteAuxiliary.toByteArray(fileNames), 0, message, 5, fileNames.length());

        return message;
    }
}
