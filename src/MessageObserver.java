import java.nio.ByteBuffer;
import java.util.UUID;

public class MessageObserver {
    private IMessageHandler handler;

    public void registerMessageHandler(IMessageHandler handler) {
        this.handler = handler;
    }

    public void handleMessage(UUID peerID, byte[] message) {
        MessageType messageType = extractMessageType(message);

        // Remove from the message type field and pass to the next handler
        byte[] newMessage = new byte[message.length - 1];
        System.arraycopy(message, 1, newMessage, 0, newMessage.length);

        switch (messageType) {
            case Handshake:
                handleHandshakeMessage(peerID, newMessage);
                break;
            case AvailablePieces:
                handleAvailablePiecesMessage(peerID, newMessage);
                break;
        }
    }

    private void handleHandshakeMessage(UUID peerID, byte[] message) {
        // Recover the peerID reported by the peer
        UUID newPeerID = ByteAuxiliary.recoverUUID(message);

        handler.handleHandshakeMessage(peerID, newPeerID);
    }

    private void handleAvailablePiecesMessage(UUID peerID, byte[] message) {
        handler.handleAvailablePiecesMessage(peerID, message);
    }

    public int extractMessageSize(byte[] message) {
        return (message.length < 4)
                ? Integer.MAX_VALUE
                : ByteBuffer.wrap(message, 0, 4).getInt();
    }

    public MessageType extractMessageType(byte[] message) {
        return MessageType.fromInteger(message[0]);
    }
}
