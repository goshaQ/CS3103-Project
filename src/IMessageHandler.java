import java.net.InetAddress;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.UUID;

public interface IMessageHandler extends ICMessageHandler, IRMessageHandler, ISMessageHandler{
}

interface ICMessageHandler {
    void handleHandshakeMessage(UUID peerID, UUID realPeerID, byte[] fileInfoHash);
    void handleAvailablePiecesMessage(UUID peerID, BitSet availablePieces);
    void handleDataRequestMessage(UUID peerID, short pieceIndex);
    void handleDataPackageMessage(UUID peerID, short pieceIndex, byte[] data);
    void handlePieceUpdateMessage(UUID peerID, short pieceIndex);
    void handleDirectoryListingReplyMessage(UUID trackerID, String directoryListing);
    void handleAnnounceReplyMessage(UUID trackerID, int status);
    void handleConnectReplyMessage(UUID trackerID, int status, FileInfo fileInfo, ArrayList<PeerInfo> peersInfo);
    void handleRelayHandshakeMessage(InetAddress inetAddress);
    void handleAllocateReplyMessage(int port);
    void handleExitPeerMessage(UUID peerID);
}

interface IRMessageHandler {
    void handleAllocateRequestMessage(UUID peerID, UUID realPeerID);
}

interface ISMessageHandler {
    void handleDirectoryListingRequestMessage(UUID packetID);
    void handleAnnounceRequestMessage(UUID packetID, PeerInfo peerInfo, FileInfo fileInfo);
    void handleConnectRequestMessage(UUID packetID, PeerInfo peerInfo, String fileName);
    void handleExitMessage(UUID packetID, UUID peerID);
}