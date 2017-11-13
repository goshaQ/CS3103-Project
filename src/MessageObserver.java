import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;

public class MessageObserver {
    private ICMessageHandler clientHandler;
    private IRMessageHandler relayServerHandler;
    private ISMessageHandler serverHandler;

    public void registerCMessageHandler(ICMessageHandler clientHandler) {
        this.clientHandler = clientHandler;
    }

    public void registerRMessageHandler(IRMessageHandler relayServerHandler) {
        this.relayServerHandler = relayServerHandler;
    }

    public void registerSMessageHandler(ISMessageHandler serverHandler) {
        this.serverHandler =  serverHandler;
    }

    public void handleMessage(UUID someID, byte[] message) {
        MessageType messageType = extractMessageType(message);

        // Remove from the message type field and pass to the next handler
        byte[] newMessage = new byte[message.length - 1];
        System.arraycopy(message, 1, newMessage, 0, newMessage.length);

        switch (messageType) {
            case Handshake:
                handleHandshakeMessage(someID, newMessage);
                break;
            case AvailablePieces:
                handleAvailablePiecesMessage(someID, newMessage);
                break;
            case DataRequest:
                handleDataRequestMessage(someID, newMessage);
                break;
            case DataPackage:
                handleDataPackageMessage(someID, newMessage);
                break;
            case PieceUpdate:
                handlePieceUpdateMessage(someID, newMessage);
                break;
            case DirectoryListingRequest:
                handleDirectoryListingRequestMessage(someID, newMessage);
                break;
            case DirectoryListingReply:
                handleDirectoryListingReplyMessage(someID, newMessage);
                break;
            case AnnounceRequest:
                handleAnnounceRequestMessage(someID, newMessage);
                break;
            case AnnounceReply:
                handleAnnounceReplyMessage(someID, newMessage);
                break;
            case ConnectRequest:
                handleConnectRequestMessage(someID, newMessage);
                break;
            case ConnectReply:
                handleConnectReplyMessage(someID, newMessage);
                break;
            case Exit:
                handleExitMessage(someID, newMessage);
                break;
            case RelayHandshake:
                handleRelayHandshakeMessage(someID, newMessage);
                break;
            case AllocateRequest:
                handleAllocateRequestMessage(someID, newMessage);
                break;
            case AllocateReply:
                handleAllocateReplyMessage(someID, newMessage);
                break;
            case ExitPeer:
                handleExitPeerMessage(someID, newMessage);
                break;
        }
    }

    private void handleHandshakeMessage(UUID peerID, byte[] message) {
        int offset = 0;

        // Recover the file info hash reported by the peer
        byte[] fileInfoHash = Arrays.copyOfRange(message, offset, (offset += FileInfo.SHA_1.getDigestLength()));
        // Recover the peerID reported by the peer
        UUID realPeerID = ByteAuxiliary.recoverUUID(Arrays.copyOfRange(message, offset, (offset = message.length)));

        clientHandler.handleHandshakeMessage(peerID, realPeerID, fileInfoHash);
    }

    private void handleAvailablePiecesMessage(UUID peerID, byte[] message) {
        // Recover the availablePieces reported by the peer
        BitSet availablePieces = ByteAuxiliary.recoverBitSet(message);

        clientHandler.handleAvailablePiecesMessage(peerID, availablePieces);
    }

    private void handleDataRequestMessage(UUID peerID, byte[] message) {
        // Recover the index of requested piece
        short pieceIndex = ByteAuxiliary.recoverShort(message);

        clientHandler.handleDataRequestMessage(peerID, pieceIndex);
    }

    private void handleDataPackageMessage(UUID peerID, byte[] message) {
        int offset = 0;

        // Recover the index of received piece
        short pieceIndex = ByteAuxiliary.recoverShort(Arrays.copyOfRange(message, offset, (offset += Short.BYTES)));
        // Recover the piece
        byte[] data = Arrays.copyOfRange(message, offset, (offset = message.length));

        clientHandler.handleDataPackageMessage(peerID, pieceIndex, data);
    }

    private void handlePieceUpdateMessage(UUID peerID, byte[] message) {
        // Recover the index of updated piece
        short pieceIndex = ByteAuxiliary.recoverShort(message);

        clientHandler.handlePieceUpdateMessage(peerID, pieceIndex);
    }

    private void handleDirectoryListingRequestMessage(UUID packetID, byte[] message) {
        serverHandler.handleDirectoryListingRequestMessage(packetID);
    }

    private void handleDirectoryListingReplyMessage(UUID trackerID, byte[] message) {
        // Recover the directory listing
        String directoryListing = ByteAuxiliary.recoverString(message);

        clientHandler.handleDirectoryListingReplyMessage(trackerID, directoryListing);
    }

    private void handleAnnounceRequestMessage(UUID packetID, byte[] message) {
        int offset = 0;

        // Recover the peer ID
        PeerInfo peerInfo = ByteAuxiliary.recoverPeerInfo(Arrays.copyOfRange(message, offset, (offset += PeerInfo.BYTES)));
        // Recover the file info
        FileInfo fileInfo = ByteAuxiliary.recoverFileInfo(Arrays.copyOfRange(message, offset, message.length));

        serverHandler.handleAnnounceRequestMessage(packetID, peerInfo, fileInfo);
    }

    private void handleAnnounceReplyMessage(UUID trackerID, byte[] message) {
        // Recover the status of the announcement request
        int status = message[0];

        clientHandler.handleAnnounceReplyMessage(trackerID, status);
    }

    private void handleConnectRequestMessage(UUID packetID, byte[] message) {
        int offset = 0;

        // Recover information about the peer
        PeerInfo peerInfo = ByteAuxiliary.recoverPeerInfo(Arrays.copyOfRange(message, offset, (offset += PeerInfo.BYTES)));
        // Recover the file name
        String fileName = ByteAuxiliary.recoverString(Arrays.copyOfRange(message, offset, (message.length - 1)));

        serverHandler.handleConnectRequestMessage(packetID, peerInfo, fileName);
    }

    private void handleConnectReplyMessage(UUID trackerID, byte[] message) {
        int offset = 0;

        // Recover the status of the connection request
        int status = message[0];

        ArrayList<PeerInfo> peersInfo = null;
        FileInfo fileInfo = null;
        if (message.length > 1) {
            // Recover the file info
            fileInfo = ByteAuxiliary.recoverFileInfo(Arrays.copyOfRange(message, (offset += 1), message.length));
            offset += FileInfo.BYTES + (fileInfo.fileName.length() * Byte.BYTES) + (fileInfo.pieceHashes.length * FileInfo.SHA_1.getDigestLength()) + 1;

            // Recover the list of peers in the swarm
            peersInfo = new ArrayList<>();
            while (offset < message.length){
                PeerInfo peerInfo = ByteAuxiliary.recoverPeerInfo(Arrays.copyOfRange(message, offset, (offset += PeerInfo.BYTES)));
                peersInfo.add(peerInfo);
            }
        }

        clientHandler.handleConnectReplyMessage(trackerID, status, fileInfo, peersInfo);
    }

    private void handleExitMessage(UUID packetID, byte[] message) {
        // Recover the peer ID
        UUID peerID = ByteAuxiliary.recoverUUID(message);

        serverHandler.handleExitMessage(packetID, peerID);
    }

    private void handleRelayHandshakeMessage(UUID peerID, byte[] message) {
        // Recover the public IP address
        InetAddress inetAddress = ByteAuxiliary.recoverInetAddress(message);

        clientHandler.handleRelayHandshakeMessage(inetAddress);
    }

    private void handleAllocateRequestMessage(UUID peerID, byte[] message) {
        // Recover the peer ID
        UUID realPeerID = ByteAuxiliary.recoverUUID(message);

        relayServerHandler.handleAllocateRequestMessage(peerID, realPeerID);
    }

    private void handleAllocateReplyMessage(UUID peerID, byte[] message) {
        // Recover the allocated port number
        int port = ByteAuxiliary.recoverInt(message);

        clientHandler.handleAllocateReplyMessage(port);
    }

    private void handleExitPeerMessage(UUID peerID, byte[] message) {
        clientHandler.handleExitPeerMessage(peerID);
    }

    private MessageType extractMessageType(byte[] message) {
        return MessageType.fromInteger(message[0]);
    }
}
