import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;

public class MessageObserver {
    private IMessageHandler handler;

    public void registerMessageHandler(IMessageHandler handler) {
        this.handler = handler;
    }

    public void handleMessage(UUID peerID, byte[] message) {
        MessageType messageType = extractMessageType(message);

        // Remove from the message type field and pass to the next handler
        byte[] newMessage = new byte[message.length - 1];
        System.arraycopy(message, 1, newMessage, 0, newMessage.length);

        switch (messageType) {
            case Handshake:
                handleHandshakeMessage(peerID, newMessage);
                break;
            case AvailablePieces:
                handleAvailablePiecesMessage(peerID, newMessage);
                break;
            case DataRequest:
                handleDataRequestMessage(peerID, newMessage);
                break;
            case DataPackage:
                handleDataPackageMessage(peerID, newMessage);
                break;
            case PieceUpdate:
                handlePieceUpdateMessage(peerID, newMessage);
                break;
        }
    }

    private void handleHandshakeMessage(UUID peerID, byte[] message) {
        int offset = 0;

        // Recover the file info hash reported by the peer
        byte[] fileInfoHash = Arrays.copyOfRange(message, offset, (offset += FileInfo.SHA_1.getDigestLength()));
        // Recover the peerID reported by the peer
        UUID realPeerID = ByteAuxiliary.recoverUUID(Arrays.copyOfRange(message, offset, (offset = message.length)));

        handler.handleHandshakeMessage(peerID, realPeerID, fileInfoHash);
    }

    private void handleAvailablePiecesMessage(UUID peerID, byte[] message) {
        // Recover the availablePieces reported by the peer
        BitSet availablePieces = ByteAuxiliary.recoverBitSet(message);

        handler.handleAvailablePiecesMessage(peerID, availablePieces);
    }

    private void handleDataRequestMessage(UUID peerID, byte[] message) {
        // Recover the index of requested piece
        short pieceIndex = ByteAuxiliary.recoverShort(message);

        handler.handleDataRequestMessage(peerID, pieceIndex);
    }

    private void handleDataPackageMessage(UUID peerID, byte[] message) {
        int offset = 0;

        // Recover the index of received piece
        short pieceIndex = ByteAuxiliary.recoverShort(Arrays.copyOfRange(message, offset, (offset += Short.BYTES)));
        // Recover the piece
        byte[] data = Arrays.copyOfRange(message, offset, (offset = message.length));

        handler.handleDataPackageMessage(peerID, pieceIndex, data);
    }

    private void handlePieceUpdateMessage(UUID peerID, byte[] message) {
        // Recover the index of updated piece
        short pieceIndex = ByteAuxiliary.recoverShort(message);

        handler.handlePieceUpdateMessage(peerID, pieceIndex);
    }

    public MessageType extractMessageType(byte[] message) {
        return MessageType.fromInteger(message[0]);
    }
}
