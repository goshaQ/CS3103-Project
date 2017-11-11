import java.util.*;

public class CommunicationMediator {
    private HashMap<UUID, Peer> peers = new HashMap<>();
    private Client client = null;
    private Tracker tracker = null;

    public void registerClient(Client client) {
        this.client = client;
    }

    public void registerTracker(Tracker tracker) {
        this.tracker = tracker;
    }

    public void registerPeer(Peer peer) {
        peers.put(peer.getPeerID(), peer);
    }

    public void deregisterPeer(UUID peerID) {
        client.checkOutgoingRequests(peerID);
        peers.remove(peerID);
    }

    public void sendHandshakeMessage(UUID peerID) {
        Peer peer = peers.get(peerID);
        byte[] message = MessageBuilder.buildHandshakeMessage(client.getFileInfo().hash, client.getClientID());

        peer.sendMessage(message);
    }

    public void sendAvailablePiecesMessage(UUID peerID) {
        Peer peer = peers.get(peerID);
        byte[] message = MessageBuilder.buildAvailablePiecesMessage(client.getAvailablePieces());

        peer.sendMessage(message);
    }

    public void sendDataRequestMessage(UUID peerID, short pieceIndex) {
        Peer peer = peers.get(peerID);
        byte[] message = MessageBuilder.buildDataRequestMessage(pieceIndex);

        peer.updateRequestedPieces(pieceIndex);
        peer.sendMessage(message);
    }

    public void sendDataPackageMessage(UUID peerID, short pieceIndex, byte[] data) {
        Peer peer = peers.get(peerID);
        byte[] message = MessageBuilder.buildDataPackageMessage(pieceIndex, data);

        peer.sendMessage(message);
    }

    public void sendPieceUpdateMessage(short pieceIndex) {
        byte[] message = MessageBuilder.buildPieceUpdateMessage(pieceIndex);

        for (Peer peer :  peers.values()) {
            if (!peer.getAvailablePieces().get(pieceIndex)) {
                peer.sendMessage(message);
            }
        }
    }

    public void sendDirectoryListingRequestMessage() {
        byte[] message = MessageBuilder.buildDirectoryListingRequestMessage();

        tracker.sendMessage(message);
    }

    public void sendAnnounceRequestMessage(UUID peerID, FileInfo fileInfo) {
        byte[] message = MessageBuilder.buildAnnounceRequestMessage(peerID, fileInfo);

        tracker.sendMessage(message);
    }

    public void sendConnectRequestMessage(UUID peerID, String fileName) {
        byte[] message = MessageBuilder.buildConnectRequestMessage(peerID, fileName);

        tracker.sendMessage(message);
    }

    public void sendExitMessage(UUID peerID) {
        byte[] message = MessageBuilder.buildExitMessage(peerID);

        tracker.sendMessage(message);
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

    public void dropPeer(UUID peerID) {
        Peer peer = peers.get(peerID);
        peer.disconnect();
    }
}