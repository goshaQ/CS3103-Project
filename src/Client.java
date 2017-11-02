import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.BitSet;
import java.util.Scanner;
import java.util.UUID;

public class Client implements IMessageHandler {
    private final UUID TRACKER_ID = UUID.randomUUID();
    // private final InetAddress TRACKER_IP = InetAddress.getByName("%TRACKER_IP%");
    private final int TRACKER_PORT = 1234;
    private final int SERVER_PORT = 5678;

    private UUID clientID;
    // TODO: put into the separate class "File"
    private BitSet pieces;

    private CommunicationMediator mediator;
    private MessageObserver observer;

    public Client() throws Exception{
        // Generate unique ID
        clientID = UUID.randomUUID();

        // Create communication mediator and register
        mediator = new CommunicationMediator();
        mediator.registerClient(this);

        // Create message observer and register
        observer = new MessageObserver();
        observer.registerMessageHandler(this);

        // Start listening for incoming connections
        acceptConnection();

        // Create UDP socket to communicate with the tracker
        //networkManager.addUDPConnection(TRACKER_ID);

        Scanner scanner = new Scanner (System.in);

        // Wait for a request of user
        int option = -1;
        while (option != 5) {
            System.out.println("Select an option:\n");
            System.out.println("1. Query the centralised server for list of files available.\n");
            System.out.println("2. Query the centralised server for a specific file.\n");
            System.out.println("3. Download a file by specifying the filename.\n");
            System.out.println("4. Inform availability of a new file.\n");
            System.out.println("5. Exit.\n");

            option = scanner.nextInt();
            switch (option) {
                case 1:
                    // ...
            }
        }

        scanner.close();
    }

    public void acceptConnection() {
        AsynchronousServerSocketChannel serverSocketChannel;

        // Create and bind the socket to the port number
        try {
            serverSocketChannel = AsynchronousServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(SERVER_PORT));
        } catch (IOException e) {
            System.err.println("Can not obtain the server socket.");
            return;
        }

        // Accept an incoming connection
        serverSocketChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
            @Override
            public void completed(AsynchronousSocketChannel socketChannel, Void attachment) {
                // Accept the next connection
                serverSocketChannel.accept(null, this);

                // Generate temp ID and create new peer object
                UUID peerID = UUID.randomUUID();
                Peer peer = new Peer(peerID, mediator, observer, socketChannel);

                // Register the peer
                mediator.registerPeer(peer);

                // Connect to the peer
                peer.connect();
            }

            @Override
            public void failed(Throwable exc, Void attachment) {
                System.err.println("Can not accept a new connection.");
            }
        });
    }

    @Override
    public void handleHandshakeMessage(UUID peerID, UUID newPeerID) {
        // Update current (temp) ID of the peer
        mediator.notifyAboutReceivedHandshake(peerID, newPeerID);

        // Send available pieces to the sender
        mediator.sendAvailablePiecesMessage(newPeerID);
    }

    @Override
    public void handleAvailablePiecesMessage(UUID peerID, BitSet availablePieces) {

    }

    public UUID getClientID() {
        return clientID;
    }
}
