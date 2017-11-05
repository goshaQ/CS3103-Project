import java.util.*;

public class CommunicationMediator {
    // Remove one of that
    private final int MAX_UDP_PACKET_SIZE = 65536;
    private final int MAX_BUFFER_SIZE = 256;

    private HashMap<UUID, Peer> peers = new HashMap<>();
    private Client client = null;

    public void registerClient(Client client) {
        this.client = client;
    }

    public void registerPeer(Peer peer) {
        peers.put(peer.getPeerID(), peer);
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

    public void notifyAboutReceivedHandshake(UUID oldPeerID, UUID realPeerID) {
        // Update the ID under which the peer with oldPeerID is stored in the hash map
        Peer peer = peers.remove(oldPeerID);
        peers.put(realPeerID, peer);

        // Ask the peer to update its ID with the new one
        peer.setPeerID(realPeerID);
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

    public void disconnect(UUID peerID) {
        // ...
    }

//    public void sendUDPMessage(UUID socketID, InetAddress ip, int port, byte[] message) {
//        DatagramSocket socket = (DatagramSocket) sockets.get(socketID);
//        DatagramPacket packet = new DatagramPacket(message, message.length, ip, port);
//
//        try {
//            socket.send(packet);
//        } catch (IOException e) {
//            System.err.println("Unable to send the UDP message.");
//        }
//    }

//    public byte[] receiveUDPMessage(UUID socketID) {
//        byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];
//
//        DatagramSocket socket = (DatagramSocket) sockets.get(socketID);
//        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
//
//        try {
//            socket.receive(packet);
//        } catch (IOException e) {
//            System.err.println("Unable to receive an UDP message.");
//        }
//
//        byte[] result = new byte[packet.getLength()];
//        System.arraycopy(buffer, 0, result, 0, packet.getLength());
//
//        return result;
//    }
}