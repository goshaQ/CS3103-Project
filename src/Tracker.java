import java.io.IOException;
import java.net.*;
import java.util.UUID;

public class Tracker {
    private static final InetAddress TRACKER_IP;
    private static final int TRACKER_PORT = 7777;
    static {
        TRACKER_IP = ByteAuxiliary.recoverInetAddress(new byte[]{(byte) 172, (byte) 19, (byte) 202, (byte) 186});
    }

    private static final int MAX_UDP_PACKET_SIZE = 65507;
    private byte[] buffer = new byte[MAX_UDP_PACKET_SIZE];

    private UUID trackerID;
    private DatagramSocket socket;
    private InetSocketAddress socketAddress;

    private CommunicationMediator mediator;
    private MessageObserver observer;

    public Tracker(UUID trackerID, CommunicationMediator mediator, MessageObserver observer) {
        this.trackerID = trackerID;
        this.mediator = mediator;
        this.observer = observer;
        this.socketAddress = new InetSocketAddress(TRACKER_IP, TRACKER_PORT);

        mediator.registerTracker(this);

        // Try to obtain the socket
        try {
            this.socket = new DatagramSocket(Main.LISTENING_PORT);
        }catch (SocketException e) {
            System.err.println("Can not obtain the server socket.");
        }
    }

    public void sendMessage(byte[] message) {
        DatagramPacket packet = new DatagramPacket(message, message.length);
        packet.setSocketAddress(socketAddress);

        try {
            socket.send(packet);

            // Wait for the reply
            receiveMessage();
        } catch (IOException e) {
            disconnect();
            System.err.println("Unable to send the UDP message.");
        }
    }

    private void receiveMessage() {
        try {
            // Receive the packet
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

            socket.setSoTimeout(1000);
            socket.receive(packet);

            // Store received information into the separate buffer
            byte[] data = new byte[packet.getLength()];
            System.arraycopy(packet.getData(), packet.getOffset(), data, 0, packet.getLength());

            // Handle the message
            observer.handleMessage(trackerID, data);
        } catch (IOException e) {
            if (e instanceof SocketTimeoutException) return;

            disconnect();
            System.err.println("An error occurred during read from the socket.");
        }

    }

    public void disconnect() {
        socket.close();
    }
}
