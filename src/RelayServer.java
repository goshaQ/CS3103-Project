import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

public class RelayServer implements IRMessageHandler {
    protected static final int BUFFER_SIZE = 1024;
    private static int ALLOCATED_RELAY_PORT = 11111;

    protected ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    protected ByteArrayOutputStream data = new ByteArrayOutputStream();
    private HashMap<UUID, HashMap<UUID, AsynchronousSocketChannel>> connectedPeers;
    private HashMap<UUID, AsynchronousServerSocketChannel> allocatedSockets;

    private AsynchronousServerSocketChannel serverSocketChannel;
    private CommunicationMediator mediator;
    private MessageObserver observer;

    public RelayServer() {
        System.out.println("\nRELAY MODE ACTIVE\n");

        // Create communication mediator and register
        mediator = new CommunicationMediator();
        mediator.registerRelayServer(this);

        // Create message observer and register
        observer = new MessageObserver();
        observer.registerRMessageHandler(this);

        // Create hash map of connected peers through relay transport address
        connectedPeers = new HashMap<>();
        allocatedSockets = new HashMap<>();

        // Start listening for incoming connections
        acceptConnection();

        // Wait until someone will not connect
        try {
            Thread.sleep(1000000);
        }catch (InterruptedException e) {
            System.err.println("The main thread has been interrupted.");
        }
    }

    private void acceptConnection() {
        // Create and bind the socket to the port number
        try {
            serverSocketChannel = AsynchronousServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(Main.LISTENING_PORT));
        } catch (IOException e) {
            System.err.println("Can not obtain the server socket for the relay server.");
            return;
        }

        // Accept an incoming connection
        serverSocketChannel.accept(this, new CompletionHandler<AsynchronousSocketChannel, RelayServer>() {
            @Override
            public void completed(AsynchronousSocketChannel socketChannel, RelayServer relay) {
                // Accept the next connection
                serverSocketChannel.accept(relay, this);

                // Generate random ID and create new peer object
                UUID peerID = UUID.randomUUID();
                Peer peer = new Peer(peerID, mediator, observer, socketChannel, false);

                // Connect to the peer
                peer.connect();
            }

            @Override
            public void failed(Throwable exc, RelayServer relay) {
                if (exc instanceof AsynchronousCloseException) return;

                stopAcceptConnection();
                System.err.println("Can not accept a new connection.");
            }
        });
    }

    private void stopAcceptConnection() {
        try {
            serverSocketChannel.close();
        } catch (IOException e) {
            System.err.println("Can not close connection with the server socket for the relay.");
        }
    }

    public void conveyUnwrappedMessage(UUID fromPeerID, UUID toPeerID, byte[] message) {
        AsynchronousSocketChannel socketChannel = connectedPeers.get(fromPeerID).get(toPeerID);
        socketChannel.write(ByteBuffer.wrap(message));
    }

    public void dropConnection(UUID peerID) {
        // Close connection with all connected peers to the peer
        for (AsynchronousSocketChannel socketChannel : connectedPeers.get(peerID).values()) {
            try {
                socketChannel.close();
            } catch (IOException e) {
                System.err.println("Can't disconnect from a peer.");
            }
        }

        // Stop accept new connections for the peer
        try {
            allocatedSockets.get(peerID).close();
        } catch (IOException e) {
            System.err.println("Can't disconnect from server socket allocated to a client.");
        }

        // Remove all records associated with the peer
        connectedPeers.remove(peerID);
        allocatedSockets.remove(peerID);
    }

    @Override
    public void handleAllocateRequestMessage(UUID peerID, UUID realPeerID) {
        AsynchronousServerSocketChannel sSocketChannel;

        // Create and bind the socket to the port number
        try {
            sSocketChannel = AsynchronousServerSocketChannel.open();
            sSocketChannel.bind(new InetSocketAddress(ALLOCATED_RELAY_PORT));
        } catch (IOException e) {
            System.err.println("Can not allocate a new relay address.");
            return;
        }

        // Save the allocated server socket to close it later
        allocatedSockets.put(realPeerID, sSocketChannel);
        // Update the peer ID stored in the mediator to the real one
        mediator.notifyAboutReceivedHandshake(peerID, realPeerID);
        // Create tha hash map to store incoming connections
        connectedPeers.put(realPeerID, new HashMap<>());

        // Accept an incoming connection
        sSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
                // Accept the next connection
                sSocketChannel.accept(null, this);

                socketChannel.read(buffer, null, new CompletionHandler<Integer, Void>() {
                    UUID pID = null;

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

                            // Check whether received handshake message
                            // Violates architecture
                            if (message[4] == MessageType.Handshake.getValue()) {
                                pID = ByteAuxiliary.recoverUUID(Arrays.copyOfRange(message, 25, messageLength));
                                connectedPeers.get(realPeerID).put(pID, socketChannel);
                            }

                            // Send message further
                            mediator.sendWrappedMessage(pID, realPeerID, message);

                            // Reset the buffer and write back everything that left to process
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

                        System.err.println("An error occurred during read from the socket associated with a peer.");
                        disconnect();
                    }

                    private void disconnect() {
                        try {
                            mediator.sendExitPeerMessage(pID, realPeerID);
                            connectedPeers.get(realPeerID).remove(pID);
                            socketChannel.close();
                        } catch (IOException e) {
                            System.err.println("Can't disconnect from a peer.");
                        }
                    }

                    private int extractMessageSize(byte[] message) {
                        return (message.length < 4)
                                ? Integer.MAX_VALUE
                                : ByteBuffer.wrap(message, 0, 4).getInt();
                    }
                });

            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                if (exc instanceof AsynchronousCloseException) return;

                stopAcceptConnection();
                System.err.println("Can not accept a new connection.");
            }
        });

        mediator.sendAllocateReplyMessage(realPeerID, ALLOCATED_RELAY_PORT);
        ALLOCATED_RELAY_PORT++;
    }
}
