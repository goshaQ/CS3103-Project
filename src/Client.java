import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

public class Client implements ICMessageHandler {
    private static final String DOWNLOAD_LOCATION = "/home/gosha/downloads/2";
    private static final String UPLOAD_LOCATION = "/home/gosha/downloads/1";

    private UUID trackerID;
    private UUID clientID;
    private ConcurrentLinkedQueue<DataRequest> outgoingRequests;
    private ConcurrentLinkedQueue<DataPackage> incomingPieces;

    private AsynchronousServerSocketChannel serverSocketChannel;
    private CommunicationMediator mediator;
    private MessageObserver observer;
    private FileProxy proxy;

    private boolean isStopping;

    public Client() {
        System.out.println("\nCLIENT MODE ACTIVE\n");

        // Generate unique ID
        clientID = UUID.randomUUID();

        // Create communication mediator and register
        mediator = new CommunicationMediator();
        mediator.registerClient(this);

        // Create message observer and register
        observer = new MessageObserver();
        observer.registerCMessageHandler(this);

        // Create outgoing requests queue
        outgoingRequests = new ConcurrentLinkedQueue<>();
        // Create incoming pieces queue
        incomingPieces = new ConcurrentLinkedQueue<>();

        // Start listening for incoming connections
        acceptConnection();

        // Initialize stopping signal variable
        isStopping = false;

        // Generate unique ID for the tracker
        trackerID = UUID.randomUUID();
        // Create the tracker
        new Tracker(trackerID, mediator, observer);

        String fileName;
        Scanner scanner = new Scanner(System.in);

        System.out.println("Select an option:");
        System.out.println("1. Query the centralised server for list of files available.");
        System.out.println("2. Inform availability of a new file.");
        System.out.println("3. Download a file by specifying the filename.");
        System.out.println("4. Exit.\n");

        // Wait for an input from the user
        int option = -1;
        while (option != 4) {
            option = scanner.nextInt();
            switch (option) {
                case 1:
                    mediator.sendDirectoryListingRequestMessage();
                    break;
                case 2:
                    // Wait for an input from the user
                    scanner.nextLine();
                    System.out.println("\nEnter the file name:");
                    fileName = scanner.nextLine();

                    // Create the file proxy
                    this.proxy = FileProxy.create(UPLOAD_LOCATION, fileName);

                    // Send request to the tracker
                    mediator.sendAnnounceRequestMessage(clientID, getFileInfo());

                    // Start uploading the file
                    start();
                    break;
                case 3:
                    // Wait for an input from the user
                    scanner.nextLine();
                    System.out.println("\nEnter the file name:");
                    fileName = scanner.nextLine();

                    // Send request to the tracker
                    mediator.sendConnectRequestMessage(clientID, fileName);
                    break;
                case 4:
                    mediator.sendExitMessage(clientID);
                    mediator.notifyAboutClosedConnection();
                    isStopping = true;

                    stopAcceptConnection();
                    break;
            }
        }

        scanner.close();
        if (proxy != null) {
            proxy.closeStream();
        }
    }

    private void start() {
        // Process downloads
        new Thread() {
            public void run() {
                while ((proxy.getPieces().cardinality() != proxy.getFileInfo().pieceCount) && (!isStopping)) {
                    download();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.err.println("Problem with the upload thread!");
                    }
                }

                System.out.println("The file has been downloaded.");
            }
        }.start();

        // Process uploads
        new Thread() {
            public void run() {
                while (!isStopping) {
                    upload();

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        System.err.println("Problem with the upload thread!");
                    }
                }
            }
        }.start();
    }

    private void download() {
        final int MAXIMUM_NUMBER_OF_REQUESTS = 10;

        DataPackage piece;
        while ((piece = incomingPieces.poll()) != null) {
            proxy.writePiece(piece.pieceIndex, piece.data);

            // Notify others about the new available piece
            mediator.sendPieceUpdateMessage(piece.pieceIndex);
        }

        int requestCount = 0;
        for (short pieceIndex : getSortedPieces()) {
            if (requestCount++ < MAXIMUM_NUMBER_OF_REQUESTS) {
                UUID peerID = mediator.findPeerOwningPiece(pieceIndex);
                mediator.sendDataRequestMessage(peerID, pieceIndex);
            } else {
                break;
            }
        }
    }

    private Short[] getSortedPieces() {
        // Get array of available pieces of peers
        BitSet[] availablePieces = mediator.askForAvailablePieces();

        int pieceCount = proxy.getFileInfo().pieceCount;

        // Calculate occurrences of each piece in available pieces of peers
        int zeroCount = pieceCount;
        short[] occurrences = new short[pieceCount];
        for (BitSet bitSet : availablePieces) {
            int index = -1;
            while ((index = bitSet.nextSetBit(index + 1)) != -1) {
                if (occurrences[index]++ == 0) {
                    zeroCount--;
                }
            }
        }

        // Create array of indexes
        Short[] indexes = new Short[pieceCount - zeroCount];
        for (short i = 0, j = 0; i < pieceCount; i++) {
            if (occurrences[i] != 0) {
                indexes[j++] = i;
            }
        }

        // Sort the array of indexes accordingly to occurrences of each piece
        Arrays.sort(indexes, new Comparator<Short>() {
            @Override
            public int compare(Short o1, Short o2) {
                return occurrences[o1] - occurrences[o2];
            }
        });

        return indexes;
    }

    private void upload() {
        DataRequest pieceRequest;
        while ((pieceRequest = outgoingRequests.poll()) != null) {
            byte[] data = proxy.readPiece(pieceRequest.pieceIndex);
            mediator.sendDataPackageMessage(pieceRequest.peerID, pieceRequest.pieceIndex, data);
        }
    }

    private void acceptConnection() {
        // Create and bind the socket to the port number
        try {
            serverSocketChannel = AsynchronousServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(Main.LISTENING_PORT));
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
            System.err.println("Can not close connection with the server socket.");
        }
    }

    public void checkOutgoingRequests(UUID peerID) {
        for (DataRequest dataRequest : outgoingRequests) {
            if (dataRequest.peerID.equals(peerID)) {
                outgoingRequests.remove(dataRequest);
            }
        }
    }

    @Override
    public void handleHandshakeMessage(UUID peerID, UUID realPeerID, byte[] fileInfoHash) {
        // Check whether we have contacted right peer
        if (!Arrays.equals(proxy.getFileInfo().hash, fileInfoHash)) {
            mediator.dropPeer(peerID);
            return;
        }

        // Update current (possible temp) ID of the peer
        mediator.notifyAboutReceivedHandshake(peerID, realPeerID);

        // Send available pieces to the sender
        if (proxy.getPieces().cardinality() != 0) {
            mediator.sendAvailablePiecesMessage(realPeerID);
        }
    }

    @Override
    public void handleAvailablePiecesMessage(UUID peerID, BitSet availablePieces) {
        // Update current available pieces for the peer
        mediator.notifyAboutReceivedAvailablePieces(peerID, availablePieces);
    }

    @Override
    public void handleDataRequestMessage(UUID peerID, short pieceIndex) {
        // Put the request into appropriate queue
        DataRequest dataRequest = new DataRequest(peerID, pieceIndex);
        outgoingRequests.add(dataRequest);
    }

    @Override
    public void handleDataPackageMessage(UUID peerID, short pieceIndex, byte[] data) {
        // Put the piece into appropriate queue
        DataPackage dataPackage = new DataPackage(peerID, pieceIndex, data);
        incomingPieces.add(dataPackage);
    }

    @Override
    public void handlePieceUpdateMessage(UUID peerID, short pieceIndex) {
        // Update the new available piece of the peer
        mediator.notifyAboutReceivedPieceUpdate(peerID, pieceIndex);
    }

    public UUID getClientID() {
        return clientID;
    }

    public BitSet getAvailablePieces() {
        return proxy.getPieces();
    }

    public FileInfo getFileInfo() {
        return proxy.getFileInfo();
    }

    @Override
    public void handleDirectoryListingReplyMessage(UUID trackerID, String directoryListing) {
        System.out.println("\nAvailable files at the tracker:");
        System.out.println(directoryListing);
    }

    @Override
    public void handleAnnounceReplyMessage(UUID trackerID, int status) {
        if (status == 1) {
            System.out.println("\nThe announcement was successful!");
        } else {
            System.out.println("\nThe announcement was NOT successful!");
            isStopping = true;
        }
    }

    @Override
    public void handleConnectReplyMessage(UUID trackerID, int status, FileInfo fileInfo, ArrayList<PeerInfo> peersInfo) {
        if (status == 1) {
            // Create the file proxy
            this.proxy = new FileProxy(DOWNLOAD_LOCATION, fileInfo, null);

            // Process received list of peers and connect to all of them
            for (PeerInfo peerInfo : peersInfo) {
                Peer peer = new Peer(peerInfo, mediator, observer);
                peer.connect();
            }

            // Start downloading the file
            start();
        } else {
            System.out.println("\nThe connection was NOT successful!");
            isStopping = true;
        }
    }
}

class DataRequest {
    public final UUID peerID;
    public final short pieceIndex;

    public DataRequest(UUID peerID, short pieceIndex) {
        this.peerID = peerID;
        this.pieceIndex = pieceIndex;
    }
}

class DataPackage {
    public final UUID peerID;
    public final short pieceIndex;
    public final byte[] data;

    public DataPackage(UUID peerID, short pieceIndex, byte[] data) {
        this.peerID = peerID;
        this.pieceIndex = pieceIndex;
        this.data = data;
    }
}