import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.ReadPendingException;
import java.util.Arrays;

public abstract class TCPConverstant {
    protected static final int BUFFER_SIZE = 1024;

    protected AsynchronousSocketChannel socketChannel = null;
    protected InetSocketAddress socketAddress;
    protected ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
    protected ByteArrayOutputStream data = new ByteArrayOutputStream();

    protected abstract void conveyMessage(byte[] message);

    protected TCPConverstant(AsynchronousSocketChannel socketChannel, InetSocketAddress socketAddress) {
        this.socketChannel = socketChannel;
        this.socketAddress = socketAddress;

    }

    protected void connect() {
        if (socketChannel == null) {
            try {
                socketChannel = AsynchronousSocketChannel.open();
                socketChannel.connect(socketAddress).get();
            } catch (Exception e) {
                System.err.println("Can't connect to a TCP converstant with the following address:");
                System.out.println(socketAddress.toString());
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
                    conveyMessage(Arrays.copyOfRange(message, 4, messageLength));

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
                if (exc instanceof ReadPendingException) return;

                System.err.println("An error occurred during read from the socket associated with a TCP converstant.");
                disconnect();
            }
        });
    }

    public void disconnect() {
        try {
            socketChannel.close();
        } catch (IOException e) {
            System.err.println("Can't disconnect from a TCP converstant.");
        }
    }

    public void sendMessage(byte[] message) {
        socketChannel.write(ByteBuffer.wrap(message));
    }

    protected int extractMessageSize(byte[] message) {
        return (message.length < 4)
                ? Integer.MAX_VALUE
                : ByteBuffer.wrap(message, 0, 4).getInt();
    }
}
