package console;

import shared.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static shared.Message.MessageType.TEXT;
import static shared.Message.MessageType.USER_NAME;

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
        openChanel();
        work();
    }

    private void openChanel() {
        try {
            selector = Selector.open();
            serverChanel = ServerSocketChannel.open();
            serverChanel.configureBlocking(false);
            serverChanel.socket().bind(new InetSocketAddress(HOST, PORT));
            serverChanel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
            closeQuietly(selector);
            closeQuietly(serverChanel);
        }
    }

    private void work() {
        while (true) {
            try {
                selector.select();
            } catch (IOException e) {
                e.printStackTrace();
                closeQuietly(selector);
                closeQuietly(serverChanel);
            }
            Iterator keys = selector.selectedKeys().iterator();

            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();
                keys.remove();

                if (!key.isValid()) {
                    System.out.println("Key not valid: " + key);
                    continue;
                }

                try {
                    if (key.isAcceptable()) {
                        acceptNewConnection(key);
                    } else if (key.isReadable()) {
                        readChannel(key);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    try (SocketChannel channel = (SocketChannel) key.channel()) {
                        chanelUserMapper.remove(channel);

                        key.cancel();
                        channel.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

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
        int numRead;
        numRead = channel.read(buffer);

        if (numRead == -1) {
            closeConnectionWithClient(channel);
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        Message message = (Message) deserialize(data);

        System.out.println("Got: " + message.getType() + " " + message.getText());

        workWithMessage(channel, message);
    }

    private void workWithMessage(SocketChannel channel, Message message) {
        if (message.getType().equals(USER_NAME)) {
            String userName = message.getText();
            chanelUserMapper.put(channel, userName);
            sendMessageForAll(userName, " entered in a chat");
        } else if (message.getType().equals(TEXT)) {
            String sender = chanelUserMapper.get(channel);
            sendMessageForAll(sender, message.getText());
        }
    }

    private void sendMessageForAll(String sender, String text) {
        Message messageForAll = new Message(TEXT, String.format("%s: %s", sender, text));
        byte[] bytes = serialize(messageForAll);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        chanelUserMapper.keySet().forEach(client -> {
            try {
                client.write(buffer);
                buffer.rewind();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        buffer.clear();
        System.out.println("Send: " + messageForAll.getText());
    }

    private void closeConnectionWithClient(SocketChannel channel) {
        String clientName = chanelUserMapper.get(channel);
        this.chanelUserMapper.remove(channel);

        System.out.println("Connection closed with client: " + clientName +
                " by address " + channel.socket().getRemoteSocketAddress());

        closeQuietly(channel);
    }
}
