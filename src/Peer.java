import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;

public class Peer {
    private static final int BUFFER_SIZE = 256;

    private CommunicationMediator mediator;
    private MessageObserver observer;
    private AsynchronousSocketChannel socketChannel = null;
    private InetSocketAddress socketAddress;
    private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    private ByteArrayOutputStream data = new ByteArrayOutputStream();

    private UUID peerID;
    private BitSet availablePieces;

    private boolean handshakeReceived = false;

    public Peer(UUID peerID, CommunicationMediator mediator, MessageObserver observer, InetAddress peerIP, int peerPort) {
        this.peerID = peerID;
        this.mediator = mediator;
        this.observer = observer;
        this.socketAddress = new InetSocketAddress(peerIP, peerPort);
    }

    public Peer(UUID peerID, CommunicationMediator mediator, MessageObserver observer, AsynchronousSocketChannel socketChannel) {
        this.peerID = peerID;
        this.mediator = mediator;
        this.observer = observer;
        this.socketChannel = socketChannel;
        this.socketAddress = null;
    }

    public void connect() {
        if (socketChannel == null) {
            try {
                socketChannel = AsynchronousSocketChannel.open();
                socketChannel.connect(socketAddress);
            } catch (Exception e) {
                System.err.println("Can't connect to a peer with the following ID: " + peerID.toString());
                return;
            }
        }

        socketChannel.read(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer length, Void attachment) {
                // Put bytes into the global buffer
                data.write(buffer.array(), 0, length);

                // Extract size of the received message
                int messageLength = observer.extractMessageSize(data.toByteArray());
                int dataLength;

                while ((dataLength = data.size()) >= messageLength) {
                    // Process the message
                    byte[] message = data.toByteArray();
                    observer.handleMessage(peerID, Arrays.copyOfRange(message, 4, messageLength));

                    // Reset the buffer and write back everything that left to process
                    // Warning! Possible bug
                    data.reset();
                    data.write(message, messageLength, (dataLength - messageLength));

                    // Update the length of message
                    messageLength = observer.extractMessageSize(data.toByteArray());
                }

                socketChannel.read(buffer, null, this);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                System.err.println("An error occurred during read from the socket" +
                        "associated with a peer with the following ID: " + peerID.toString());
            }
        });

        mediator.sendHandshakeMessage(peerID);
        if (handshakeReceived) {
            mediator.sendAvailablePiecesMessage(peerID);
        }
    }

    public void sendMessage(byte[] message) {
        socketChannel.write(ByteBuffer.wrap(message));
    }

    public UUID getPeerID() {
        return peerID;
    }

    public void setPeerID(UUID peerID) {
        this.peerID = peerID;
    }

    public BitSet getAvailablePieces() {
        return availablePieces;
    }

    public void setAvailablePieces(BitSet availablePieces) {
        this.availablePieces = availablePieces;
    }

    public boolean isHandshakeReceived() {
        return handshakeReceived;
    }

    public void setHandshakeReceived(boolean handshakeReceived) {
        this.handshakeReceived = handshakeReceived;
    }
}
