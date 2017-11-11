import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Arrays;
import java.util.BitSet;
import java.util.UUID;

public class Peer {
    private static final int BUFFER_SIZE = 1024;

    private CommunicationMediator mediator;
    private MessageObserver observer;
    private AsynchronousSocketChannel socketChannel = null;
    private InetSocketAddress socketAddress;
    private ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    private ByteArrayOutputStream data = new ByteArrayOutputStream();

    private UUID peerID;
    private BitSet availablePieces;
    private BitSet requestedPieces;

    public Peer(PeerInfo peerInfo, CommunicationMediator mediator, MessageObserver observer) {
        this.peerID = peerInfo.peerID;
        this.mediator = mediator;
        this.observer = observer;
        this.availablePieces = new BitSet();
        this.requestedPieces = new BitSet();
        this.socketAddress = new InetSocketAddress(peerInfo.inetAddress, peerInfo.port);

        mediator.registerPeer(this);
    }

    public Peer(UUID peerID, CommunicationMediator mediator, MessageObserver observer, AsynchronousSocketChannel socketChannel) {
        this.peerID = peerID;
        this.mediator = mediator;
        this.observer = observer;
        this.availablePieces = new BitSet();
        this.requestedPieces = new BitSet();
        this.socketChannel = socketChannel;
        this.socketAddress = null;

        mediator.registerPeer(this);
    }

    public void connect() {
        if (socketChannel == null) {
            try {
                socketChannel = AsynchronousSocketChannel.open();
                socketChannel.connect(socketAddress).get();
            } catch (Exception e) {
                System.err.println("Can't connect to a peer with the following ID: " + peerID.toString());
                return;
            }
        }

        socketChannel.read(buffer, null, new CompletionHandler<Integer, Void>() {
            @Override
            public void completed(Integer length, Void attachment) {
                // Check whether the peer has disconnected
                if (length < 1) {
                    disconnect();
                    return;
                }

                // Put bytes into the global buffer
                data.write(buffer.array(), 0, length);

                // Extract size of the received message
                int messageLength = extractMessageSize(data.toByteArray());
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
                    messageLength = extractMessageSize(data.toByteArray());
                }

                buffer.clear();
                socketChannel.read(buffer, null, this);
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                if (exc instanceof AsynchronousCloseException) return;

                System.err.println("An error occurred during read from the socket" +
                        "associated with a peer with the following ID: " + peerID.toString());
                disconnect();
            }
        });

        mediator.sendHandshakeMessage(peerID);
    }

    public void disconnect() {
        try {
            mediator.deregisterPeer(peerID);
            socketChannel.close();
        } catch (IOException e) {
            System.err.println("Can't disconnect from a peer with the following ID: " + peerID.toString());
        }
    }

    public void sendMessage(byte[] message) {
        socketChannel.write(ByteBuffer.wrap(message));
    }

    private int extractMessageSize(byte[] message) {
        return (message.length < 4)
                ? Integer.MAX_VALUE
                : ByteBuffer.wrap(message, 0, 4).getInt();
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

    public BitSet getRequestedPieces() {
        return requestedPieces;
    }

    public void setAvailablePieces(BitSet availablePieces) {
        this.availablePieces = availablePieces;
    }

    public void updateAvailablePieces(short pieceIndex) {
        availablePieces.flip(pieceIndex);
    }

    public void  updateRequestedPieces(short pieceIndex) {
        requestedPieces.flip(pieceIndex);
    }
}
