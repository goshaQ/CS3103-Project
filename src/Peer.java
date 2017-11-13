import sun.plugin2.message.Message;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.BitSet;
import java.util.UUID;

public class Peer extends TCPConverstant {
    private CommunicationMediator mediator;
    private MessageObserver observer;

    private UUID peerID;
    private BitSet availablePieces;
    private BitSet requestedPieces;

    private boolean isThroughRelay;

    public Peer(PeerInfo peerInfo, CommunicationMediator mediator, MessageObserver observer) {
        super(null, new InetSocketAddress(peerInfo.inetAddress, peerInfo.port));

        this.peerID = peerInfo.peerID;
        this.mediator = mediator;
        this.observer = observer;
        this.availablePieces = new BitSet();
        this.requestedPieces = new BitSet();
        this.isThroughRelay = false;

        mediator.registerPeer(this);
    }

    public Peer(UUID peerID, CommunicationMediator mediator, MessageObserver observer, AsynchronousSocketChannel socketChannel, boolean isThroughRelay) {
        super(socketChannel, null);

        this.peerID = peerID;
        this.mediator = mediator;
        this.observer = observer;
        this.availablePieces = new BitSet();
        this.requestedPieces = new BitSet();
        this.isThroughRelay = isThroughRelay;

        mediator.registerPeer(this);
    }

    public void connect() {
        if (!isThroughRelay) super.connect();
        mediator.sendHandshakeMessage(peerID);
    }

    public void disconnect() {
        if (!isThroughRelay) super.disconnect();
        mediator.deregisterPeer(peerID);
    }

    @Override
    protected void conveyMessage(byte[] message) {
        if (mediator.clientExists()) {
            observer.handleMessage(peerID, message);
        } else {
            // Check whether received message that the relay server should process
            // Violates architecture
            if (message[0] == MessageType.AllocateRequest.getValue()) {
                observer.handleMessage(peerID, message);
            } else {
                mediator.sendUnwrappedMessage(peerID, message);
            }
        }
    }

    public boolean isThroughRelay() {
        return isThroughRelay;
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

    public InetAddress getInetAddress() {
        InetAddress tmp = null;
        try {
            tmp = ((InetSocketAddress) socketChannel.getRemoteAddress()).getAddress();
        } catch (IOException e) {
            System.err.println("Can not get the peer's inet address.");
        }

        return tmp;
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