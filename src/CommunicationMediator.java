import java.net.InetSocketAddress;
import java.util.*;

public class CommunicationMediator {
    private HashMap<UUID, Peer> peers = new HashMap<>();
    private Client client = null;
    private RelayServer relayServer = null;
    private Relay relay = null;
    private Tracker tracker = null;

    public void registerClient(Client client) {
        this.client = client;
    }

    public void registerRelayServer(RelayServer relayServer) {
        this.relayServer = relayServer;
    }

    public void registerRelay(Relay relay) {
        this.relay = relay;
    }

    public void registerTracker(Tracker tracker) {
        this.tracker = tracker;
    }

    public void registerPeer(Peer peer) {
        peers.put(peer.getPeerID(), peer);
    }

    public void deregisterPeer(UUID peerID) {
        if (client != null) {
            client.checkOutgoingRequests(peerID);
        } else {
            relayServer.dropConnection(peerID);
        }

        peers.remove(peerID);
    }

    public void sendHandshakeMessage(UUID peerID) {
        Peer peer = peers.get(peerID);

        byte[] message;
        if (client != null) {
            message = MessageBuilder.buildHandshakeMessage(client.getFileInfo().hash, client.getClientID());
        } else {
            message = MessageBuilder.buildRelayHandshakeMessage(peer.getInetAddress());
            message = MessageBuilder.buildRelayWrappedMessage(peerID, message);
        }

        sendMessage(peer, message);
    }

    public void sendAvailablePiecesMessage(UUID peerID) {
        Peer peer = peers.get(peerID);
        byte[] message = MessageBuilder.buildAvailablePiecesMessage(client.getAvailablePieces());

        sendMessage(peer, message);
    }

    public void sendDataRequestMessage(UUID peerID, short pieceIndex) {
        Peer peer = peers.get(peerID);
        byte[] message = MessageBuilder.buildDataRequestMessage(pieceIndex);

        peer.updateRequestedPieces(pieceIndex);
        sendMessage(peer, message);
    }

    public void sendDataPackageMessage(UUID peerID, short pieceIndex, byte[] data) {
        Peer peer = peers.get(peerID);
        byte[] message = MessageBuilder.buildDataPackageMessage(pieceIndex, data);

        sendMessage(peer, message);
    }

    public void sendPieceUpdateMessage(short pieceIndex) {
        byte[] message = MessageBuilder.buildPieceUpdateMessage(pieceIndex);

        for (Peer peer :  peers.values()) {
            if (!peer.getAvailablePieces().get(pieceIndex)) {
                sendMessage(peer, message);
            }
        }
    }

    public void sendDirectoryListingRequestMessage() {
        byte[] message = MessageBuilder.buildDirectoryListingRequestMessage();

        tracker.sendMessage(message);
    }

    public void sendAnnounceRequestMessage(UUID peerID, InetSocketAddress socketAddress, FileInfo fileInfo) {
        PeerInfo peerInfo = new PeerInfo(peerID, socketAddress.getAddress(), socketAddress.getPort());
        byte[] message = MessageBuilder.buildAnnounceRequestMessage(peerInfo, fileInfo);

        tracker.sendMessage(message);
    }

    public void sendConnectRequestMessage(UUID peerID, InetSocketAddress socketAddress, String fileName) {
        PeerInfo peerInfo = new PeerInfo(peerID, socketAddress.getAddress(), socketAddress.getPort());
        byte[] message = MessageBuilder.buildConnectRequestMessage(peerInfo, fileName);

        tracker.sendMessage(message);
    }

    public void sendExitMessage(UUID peerID) {
        byte[] message = MessageBuilder.buildExitMessage(peerID);

        tracker.sendMessage(message);
    }

    public void sendAllocateRequestMessage(UUID peerID) {
        byte[] message = MessageBuilder.buildAllocateRequestMessage(peerID);

        relay.sendMessage(message);
    }

    public void sendAllocateReplyMessage(UUID peerID, int port) {
        Peer peer = peers.get(peerID);
        byte[] message = MessageBuilder.buildAllocateReplyMessage(port);
        message = MessageBuilder.buildRelayWrappedMessage(peerID, message);

        peer.sendMessage(message);
    }

    public void sendExitPeerMessage(UUID fromPeerID, UUID toPeerID) {
        Peer peer = peers.get(toPeerID);
        byte[] message = MessageBuilder.buildPeerExitMessage();
        message = MessageBuilder.buildRelayWrappedMessage(fromPeerID, message);

        peer.sendMessage(message);
    }

    public void sendWrappedMessage(UUID fromPeerID, UUID toPeerID, byte[] message) {
        Peer peer = peers.get(toPeerID);
        byte[] wrappedMessage = MessageBuilder.buildRelayWrappedMessage(fromPeerID, message);

        peer.sendMessage(wrappedMessage);
    }

    public void sendUnwrappedMessage(UUID fromPeerID, byte[] message) {
        UUID toPeerID = ByteAuxiliary.recoverUUID(Arrays.copyOfRange(message, 0, (2 * Long.BYTES)));
        byte[] unwrappedMessage = MessageBuilder.buildRelayUnwrappedMessage(message);

        relayServer.conveyUnwrappedMessage(fromPeerID, toPeerID, unwrappedMessage);
    }

    private void sendMessage(Peer peer, byte[] message) {
        if (!peer.isThroughRelay()) {
            peer.sendMessage(message);
        } else {
            relay.sendMessage(peer.getPeerID(), message);
        }
    }

    public void notifyAboutReceivedHandshake(UUID oldPeerID, UUID realPeerID) {
        if (oldPeerID != realPeerID) {
            // Update the ID under which the peer with oldPeerID is stored in the hash map
            Peer peer = peers.remove(oldPeerID);
            peers.put(realPeerID, peer);

            // Ask the peer to update its ID with the new one
            peer.setPeerID(realPeerID);
        }
    }

    public void notifyAboutReceivedAvailablePieces(UUID peerID, BitSet availablePieces) {
        // Update available pieces for the peer
        Peer peer = peers.get(peerID);
        peer.setAvailablePieces(availablePieces);
    }

    public void notifyAboutReceivedPieceUpdate(UUID peerID, short pieceIndex) {
        // Update the new available piece of the peer
        Peer peer = peers.get(peerID);
        peer.updateAvailablePieces(pieceIndex);
    }

    public void notifyAboutClosedConnection() {
        // Safe disconnect from each peer
        for (Peer peer :  peers.values()) {
            peer.disconnect();
        }

        tracker.disconnect();
    }

    public BitSet[] askForAvailablePieces() {
        BitSet[] availablePieces = new BitSet[peers.size()];

        int i = 0;
        for (Peer peer : peers.values()) {
            BitSet bitSet = (BitSet) peer.getAvailablePieces().clone();
            bitSet.andNot(peer.getRequestedPieces());
            bitSet.andNot(client.getAvailablePieces());

            availablePieces[i++] = bitSet;
        }

        return availablePieces;
    }

    public UUID findPeerOwningPiece(short pieceIndex) {
        ArrayList<Peer> arrayList = new ArrayList<>(peers.values());
        Collections.shuffle(arrayList);

        for (Peer peer : arrayList) {
            if (peer.getAvailablePieces().get(pieceIndex)) {
                return peer.getPeerID();
            }
        }

        return null;
    }

    public boolean peerExists(UUID peerID) {
        return peers.containsKey(peerID);
    }

    public boolean clientExists() {
        return client != null;
    }


    public void dropPeer(UUID peerID) {
        Peer peer = peers.get(peerID);
        peer.disconnect();
    }
}