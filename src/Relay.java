import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.UUID;

public class Relay extends TCPConverstant{
    public static final InetAddress RELAY_IP;
    private static final int RELAY_PORT = 8888;
    static {
        RELAY_IP = ByteAuxiliary.recoverInetAddress(new byte[]{(byte) 172, (byte) 19, (byte) 202, (byte) 186});
    }

    private CommunicationMediator mediator;
    private MessageObserver observer;

    public Relay(CommunicationMediator mediator, MessageObserver observer) {
        super(null, new InetSocketAddress(RELAY_IP, RELAY_PORT));

        this.mediator = mediator;
        this.observer = observer;

        mediator.registerRelay(this);
    }

    public void sendMessage(UUID peerID, byte[] message) {
        byte[] wrappedMessage = MessageBuilder.buildRelayWrappedMessage(peerID, message);
        super.sendMessage(wrappedMessage);
    }

    @Override
    protected void conveyMessage(byte[] message) {
        UUID peerID = extractUUID(message);
        observer.handleMessage(peerID, Arrays.copyOfRange(message, (2 * Long.BYTES), message.length));
    }

    private UUID extractUUID(byte[] message) {
        return ByteAuxiliary.recoverUUID(Arrays.copyOfRange(message, 0, (2 * Long.BYTES)));
    }
}
