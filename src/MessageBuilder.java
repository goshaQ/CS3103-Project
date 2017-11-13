import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
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
        int length = 1;

        byte[] message = new byte[length];
        // Write the type of message
        message[0] = (byte) MessageType.DirectoryListingRequest.getValue();

        return message;
    }

    public static byte[] buildDirectoryListingReplyMessage(String fileNames) {
        int offset = 0, length = 1 + fileNames.length();

        byte[] message = new byte[length];
        // Write the type of message
        message[0] = (byte) MessageType.DirectoryListingReply.getValue();
        // Write the provided file names
        System.arraycopy(ByteAuxiliary.toByteArray(fileNames), 0, message, (offset += 1), fileNames.length());

        return message;
    }

    public static byte[] buildAnnounceRequestMessage(PeerInfo peerInfo, FileInfo fileInfo) {
        int fileInfoLength = FileInfo.BYTES + (fileInfo.fileName.length() * Byte.BYTES) + (fileInfo.pieceHashes.length * FileInfo.SHA_1.getDigestLength());
        int offset = 0, length = 2 + PeerInfo.BYTES + fileInfoLength;

        byte[] message = new byte[length];
        // Write the type of message
        message[0] = (byte) MessageType.AnnounceRequest.getValue();
        // Write information about the client
        System.arraycopy(ByteAuxiliary.toByteArray(peerInfo), 0, message, (offset += 1), PeerInfo.BYTES);
        // Write information about the file
        System.arraycopy(ByteAuxiliary.toByteArray(fileInfo), 0, message, (offset += PeerInfo.BYTES), (fileInfoLength + 1));

        return message;
    }

    public static byte[] buildAnnounceReplyMessage(int status) {
        int length = 2;

        byte[] message = new byte[length];
        // Write the type of message
        message[0] = (byte) MessageType.AnnounceReply.getValue();
        // Write the status of the announce request
        message[1] = (byte) status;

        return message;
    }

    public static byte[] buildConnectRequestMessage(PeerInfo peerInfo, String fileName) {
        int offset = 0, length = 2 + PeerInfo.BYTES + fileName.length();

        byte[] message = new byte[length];
        // Write the type of message
        message[0] = (byte) MessageType.ConnectRequest.getValue();
        // Write the information about the client
        System.arraycopy(ByteAuxiliary.toByteArray(peerInfo), 0, message, (offset += 1), PeerInfo.BYTES);
        // Write the file name
        System.arraycopy(ByteAuxiliary.toByteArray(fileName + '\n'), 0, message, (offset += PeerInfo.BYTES), fileName.length() + 1);

        return message;
    }

    public static byte[] buildConnectReplyMessage(int status, FileInfo fileInfo, ArrayList<PeerInfo> peersInfo) {
        int fileInfoLength = (fileInfo != null) ? FileInfo.BYTES + (fileInfo.fileName.length() * Byte.BYTES) + (fileInfo.pieceHashes.length * FileInfo.SHA_1.getDigestLength()) : 0;
        int offset = 0, length = 2 + ((peersInfo != null) ? (PeerInfo.BYTES * peersInfo.size()) + fileInfoLength + 1 : 0);

        byte[] message = new byte[length];
        // Write the type of message
        message[0] = (byte) MessageType.ConnectReply.getValue();
        // Write the status of the connect request
        message[1] = (byte) status;

        if (peersInfo != null) {
            // Write information about the file
            System.arraycopy(ByteAuxiliary.toByteArray(fileInfo), 0, message, (offset += 2), (fileInfoLength + 1));
            offset += (fileInfoLength + 1);

            // Write information about all peers currently uploading / downloading the file
            for (PeerInfo peerInfo : peersInfo) {
                System.arraycopy(ByteAuxiliary.toByteArray(peerInfo), 0, message, offset, PeerInfo.BYTES);
                offset += PeerInfo.BYTES;
            }
        }

        return message;
    }

    public static byte[] buildExitMessage(UUID peerID) {
        int length = 1 + (2 * Long.BYTES);

        byte[] message = new byte[length];
        // Write the type of message
        message[0] = (byte) MessageType.Exit.getValue();
        // Write the peer ID
        System.arraycopy(ByteAuxiliary.toByteArray(peerID), 0, message, 1, (2 * Long.BYTES));

        return message;
    }

    public static byte[] buildRelayHandshakeMessage(InetAddress inetAddress) {
        int offset = 0, length = 5 + Integer.BYTES;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, offset, Integer.BYTES);
        // Write the type of message
        message[(offset += Integer.BYTES)] = (byte) MessageType.RelayHandshake.getValue();
        // Write the peer IP address
        System.arraycopy(inetAddress.getAddress(), 0, message, (offset += 1), Integer.BYTES);

        return message;
    }

    public static byte[] buildAllocateRequestMessage(UUID peerID) {
        int offset = 0, length = 5 + (4 * Long.BYTES);

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, offset, Integer.BYTES);
        // Write the type of message
        message[(offset += Integer.BYTES)] = (byte) MessageType.AllocateRequest.getValue();
        // Write the sender peer ID
        System.arraycopy(ByteAuxiliary.toByteArray(peerID), 0, message, (offset += 1), (2 * Long.BYTES));

        return message;
    }

    public static byte[] buildAllocateReplyMessage(int port) {
        int offset = 0, length = 5 + (2 * Long.BYTES) + Integer.BYTES;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, offset, Integer.BYTES);
        // Write the type of message
        message[(offset += Integer.BYTES)] = (byte) MessageType.AllocateReply.getValue();
        // Write the allocated port number
        System.arraycopy(ByteAuxiliary.toByteArray(port), 0, message, (offset += 1), Integer.BYTES);

        return message;
    }

    public static byte[] buildPeerExitMessage() {
        int offset = 0, length = 5;

        byte[] message = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, message, offset, Integer.BYTES);
        // Write the type of message
        message[(offset += Integer.BYTES)] = (byte) MessageType.ExitPeer.getValue();

        return message;
    }

    public static byte[] buildRelayWrappedMessage(UUID fromPeerID, byte[] message) {
        int offset = 0, length = (2 * Long.BYTES) + message.length;

        byte[] wrappedMessage = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, wrappedMessage, offset, Integer.BYTES);
        // Write the sender peer ID
        System.arraycopy(ByteAuxiliary.toByteArray(fromPeerID), 0, wrappedMessage, (offset += Integer.BYTES), (2 * Long.BYTES));
        // Write the rest of the message
        System.arraycopy(message, 4, wrappedMessage, (offset += (2 * Long.BYTES)), (message.length - 4));

        return wrappedMessage;
    }

    public static byte[] buildRelayUnwrappedMessage(byte[] message) {
        int offset = 0, length =  Integer.BYTES + message.length - (2 * Long.BYTES);

        byte[] unwrappedMessage = new byte[length];
        // Write the size of message
        System.arraycopy(ByteAuxiliary.toByteArray(length), 0, unwrappedMessage, offset, Integer.BYTES);
        // Write the rest of the message except the sender peer ID
        System.arraycopy(message, (2 * Long.BYTES), unwrappedMessage, (offset += Integer.BYTES), (message.length - (2 * Long.BYTES)));

        return unwrappedMessage;
    }
}