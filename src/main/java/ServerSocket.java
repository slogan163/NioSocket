import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;

public class ServerSocket {
    private Selector selector;
    private Map<SocketChannel, List> dataMapper;
    private InetSocketAddress listenAddress;

    public ServerSocket(String address, int port) {
        listenAddress = new InetSocketAddress(address, port);
        dataMapper = new HashMap<>();
    }

    public static void main(String[] args) throws Exception {
        Runnable server = () -> {
            try {
                new ServerSocket("localhost", 8090).startServer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        };

        Runnable client = () -> {
            try {
                new ClientSocket().startClient();
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        new Thread(server).start();
        new Thread(client, "client-A").start();
        new Thread(client, "client-B").start();

    }

    private void startServer() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel serverChanel = ServerSocketChannel.open();
        serverChanel.configureBlocking(false);

        serverChanel.socket().bind(listenAddress);
        serverChanel.register(this.selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started...");

        while (true) {
            this.selector.select();

            Iterator keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    this.accept(key);
                } else if (key.isReadable()) {
                    this.read(key);
                }
            }
        }
    }


    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        System.out.println("Connected to: " + remoteAddr);

        // register channel with selector for further IO
        dataMapper.put(channel, new ArrayList());
        channel.register(this.selector, SelectionKey.OP_READ);
    }

    //read from the socket channel
    private void read(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = -1;
        numRead = channel.read(buffer);

        if (numRead == -1) {
            this.dataMapper.remove(channel);
            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            System.out.println("Connection closed by client: " + remoteAddr);
            channel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        System.out.println("Got: " + new String(data));
    }
}
