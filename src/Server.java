import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Server implements ISMessageHandler {
    private static final int MAX_UDP_PACKET_SIZE = 65507;
    private final int LISTENING_PORT = 7777;

    private byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];

    private HashMap<UUID, DatagramPacket> packets = new HashMap<>();
    private HashMap<UUID, PeerInfo> peersInfo = new HashMap<>();
    private ConcurrentLinkedQueue<Swarm> swarms = new ConcurrentLinkedQueue<>();

    private DatagramSocket socket;
    private MessageObserver observer;

    public Server() {
        System.out.println("\nSERVER MODE ACTIVE\n");

        // Create message observer and register
        observer = new MessageObserver();
        observer.registerSMessageHandler(this);

        // Try to obtain the server socket
        try {
            socket = new DatagramSocket(LISTENING_PORT);
            receiveMessage();
        }catch (SocketException e) {
                System.err.println("Can not obtain the server socket.");
        }
    }

    private void receiveMessage() {
        try {
            while (true) {
                // Create temporary ID for the packet
                UUID packetID = UUID.randomUUID();

                // Receive the packet
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                // Store received information into the separate buffer
                byte[] data = new byte[packet.getLength()];
                System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

                // Put the packet into the temporary hash map
                packets.put(packetID, packet);

                // Create new thread to handle the message
                new Thread() {
                    public void run() {
                        observer.handleMessage(packetID, data);
                    }
                }.start();
            }
        } catch (IOException e) {
            System.err.println("An error occurred during read from the socket.");
        }
    }

    private void sendMessage(UUID packetID, byte[] message) {
        DatagramPacket packet = packets.remove(packetID);
        packet.setData(message);

        try {
            socket.send(packet);
        } catch (IOException e) {
            System.err.println("Unable to send the UDP message.");
        }
    }

    private String listFiles() {
        final int MAXIMUM_NUMBER_OF_LISTED_FILES = 50;

        StringBuilder stringBuilder = new StringBuilder();
        int listedFileCount = 0;

        for (Swarm swarm : swarms) {
            stringBuilder.append('\t');

            if (listedFileCount++ < MAXIMUM_NUMBER_OF_LISTED_FILES) {
                stringBuilder.append(swarm.fileInfo.fileName);
            } else {
                stringBuilder.append("...");
                break;
            }

            stringBuilder.append('\n');
        }

        return stringBuilder.toString();
    }

    @Override
    public void handleDirectoryListingRequestMessage(UUID packetID) {
        String list = listFiles();

        byte[] message = MessageBuilder.buildDirectoryListingReplyMessage(list);
        sendMessage(packetID, message);
    }

    @Override
    public void handleAnnounceRequestMessage(UUID packetID, UUID peerID, FileInfo fileInfo) {
        int status = 1;

        // Check whether the file announcement is duplicate
        for (Swarm swarm : swarms) {
            if (Arrays.equals(fileInfo.hash, swarm.fileInfo.hash)) {
                status = 0;
                break;
            }
        }

        if (status != 0) {
            // Add the peer to the peers info hash map
            if (!peersInfo.containsKey(peerID)) {
                DatagramPacket packet = packets.get(packetID);
                PeerInfo peerInfo = new PeerInfo(peerID, packet.getAddress(), packet.getPort());

                peersInfo.put(peerID, peerInfo);
            }

            // Create new swarm
            Swarm swarm = new Swarm(fileInfo);
            swarm.addPeer(peerID);

            // Put into the swarms hash map
            swarms.add(swarm);
        }

        byte[] message = MessageBuilder.buildAnnounceReplyMessage(status);
        sendMessage(packetID, message);
    }

    @Override
    public void handleConnectRequestMessage(UUID packetID, UUID peerID, String fileName) {
        // Check whether the swarm for the file exists
        Swarm swarm = null;
        for (Swarm s : swarms) {
            if (s.fileInfo.fileName.equals(fileName)) {
                swarm = s;
                break;
            }
        }

        int status = 0;
        ArrayList<PeerInfo> pInfo = null;
        FileInfo fInfo = null;
        if (swarm != null) {
            // Update the status
            status = 1;

            // Get the list of peers in the swarm
            pInfo = new ArrayList<>();
            for (UUID pID : swarm.peerIDs) {
                PeerInfo peerInfo = peersInfo.get(pID);
                pInfo.add(peerInfo);
            }

            // Get the file info
            fInfo = swarm.fileInfo;

            // Add the peer to the peers info hash map
            if (!peersInfo.containsKey(peerID)) {
                DatagramPacket packet = packets.get(packetID);
                PeerInfo peerInfo = new PeerInfo(peerID, packet.getAddress(), packet.getPort());

                peersInfo.put(peerID, peerInfo);
            }

            // Add the peer to the swarm
            swarm.addPeer(peerID);
        }

        byte[] message = MessageBuilder.buildConnectReplyMessage(status, fInfo, pInfo);
        sendMessage(packetID, message);
    }

    @Override
    public void handleExitMessage(UUID packetID, UUID peerID) {
        // Remove the packet from the hash map
        packets.remove(packetID);

        // Find swarms where the peer appears and remove
        for (Swarm swarm : swarms) {
            if (swarm.peerIDs.contains(peerID)) {
                swarm.removePeer(peerID);

                // Check whether the swarm is destroyed
                if (swarm.peerIDs.isEmpty()) {
                    swarms.remove(swarm);
                }
            }
        }

        // Remove the peer from the hash map
        peersInfo.remove(peerID);
    }
}
