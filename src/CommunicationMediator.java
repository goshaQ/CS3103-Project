import java.util.HashMap;
import java.util.UUID;

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
        byte[] message = MessageBuilder.buildHandshakeMessage(client.getClientID());

        peer.sendMessage(message);
    }

    public void sendAvailablePiecesMessage(UUID peerID) {
        Peer peer = peers.get(peerID);
        byte[] message = MessageBuilder.buildAvailablePiecesMessage(peerID, peer.getAvailablePieces());

        peer.sendMessage(message);
    }

    public void notifyAboutReceivedHandshake(UUID oldPeerID, UUID newPeerID) {
        // Update the ID under which the peer with oldPeerID is stored in the hash map
        Peer peer = peers.remove(oldPeerID);
        peers.put(newPeerID, peer);

        // Notify the peer about received handshake
        peer.setHandshakeReceived(true);

        // Ask the peer to update its ID with the new one
        peer.setPeerID(newPeerID);
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