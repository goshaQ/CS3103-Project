import java.net.InetAddress;
import java.util.UUID;

public class PeerInfo {
    public static final int BYTES = (2 * Long.BYTES) + Integer.BYTES + Integer.BYTES;

    public final UUID peerID;
    public final InetAddress inetAddress;
    public final int port;

    public PeerInfo(UUID peerID, InetAddress inetAddress, int port) {
        this.peerID = peerID;
        this.inetAddress = inetAddress;
        this.port = port;
    }

}
