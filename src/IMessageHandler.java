import java.util.BitSet;
import java.util.UUID;

public interface IMessageHandler {
    void handleHandshakeMessage(UUID peerID, UUID realPeerID, byte[] fileInfoHash);
    void handleAvailablePiecesMessage(UUID peerID, BitSet availablePieces);
    void handleDataRequestMessage(UUID peerID, short pieceIndex);
    void handleDataPackageMessage(UUID peerID, short pieceIndex, byte[] data);
    void handlePieceUpdateMessage(UUID peerID, short pieceIndex);
}
