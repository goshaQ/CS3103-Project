import java.util.BitSet;
import java.util.UUID;

public interface IMessageHandler {
    void handleHandshakeMessage(UUID peerID, UUID newPeerID);
    void handleAvailablePiecesMessage(UUID peerID, BitSet availablePieces);
}
