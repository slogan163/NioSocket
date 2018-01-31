package console;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.commons.io.IOUtils.closeQuietly;

public class ServerTerminal {

    public final String HOST = "localhost";
    public final int PORT = 8090;

    private Selector selector;
    private ServerSocketChannel serverChanel;
    private Map<SocketChannel, String> chanelUserMapper = new HashMap<>();

    public static void main(String[] args) throws IOException {
        new ServerTerminal().start();
    }

    public void start() {
        try {
            openChanel();
            work();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(selector);
            closeQuietly(serverChanel);
        }
    }

    private void openChanel() throws IOException {
        selector = Selector.open();
        serverChanel = ServerSocketChannel.open();
        serverChanel.configureBlocking(false);
        serverChanel.socket().bind(new InetSocketAddress(HOST, PORT));
        serverChanel.register(selector, SelectionKey.OP_ACCEPT);
    }

    private void work() throws IOException {
        while (true) {
            selector.select();
            Iterator keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();
                keys.remove();

                if (!key.isValid()) {
                    System.out.println("Key not valid: " + key);
                    continue;
                }

                if (key.isAcceptable()) {
                    acceptNewConnection(key);
                } else if (key.isReadable()) {
                    readChannel(key);
                }
            }
        }
    }

    private void acceptNewConnection(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);

        System.out.println("New connection by address: " + channel.socket().getRemoteSocketAddress());

        chanelUserMapper.put(channel, "");
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    private void readChannel(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = -1;
        numRead = channel.read(buffer);

        if (numRead == -1) {
            closeConnectionWithClient(channel);
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        System.out.println("Got: " + new String(data));
    }

    private void closeConnectionWithClient(SocketChannel channel) {
        String clientName = chanelUserMapper.get(channel);
        this.chanelUserMapper.remove(channel);

        System.out.println("Connection closed with client: " + clientName +
                " by address " + channel.socket().getRemoteSocketAddress());

        closeQuietly(channel);
    }
}
