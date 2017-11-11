import java.util.ArrayList;
import java.util.UUID;

public class Swarm {
    public final FileInfo fileInfo;
    public final ArrayList<UUID> peerIDs;

    public Swarm(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
        this.peerIDs = new ArrayList<>();
    }

    public void addPeer(UUID peerID) {
        peerIDs.add(peerID);
    }

    public void removePeer(UUID peerID) {
        peerIDs.remove(peerID);
    }

}
