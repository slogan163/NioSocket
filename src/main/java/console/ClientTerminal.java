package console;

import shared.Message;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

import static org.apache.commons.lang3.SerializationUtils.deserialize;
import static org.apache.commons.lang3.SerializationUtils.serialize;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static shared.Message.MessageType.TEXT;
import static shared.Message.MessageType.USER_NAME;

public class ClientTerminal {

    static String CLIENT_NAME = "anon";
    static InetSocketAddress HOST_ADDRESS = new InetSocketAddress("localhost", 8090);

    public static void main(String[] args) throws IOException {
        new ClientTerminal().start();
    }

    private void start() throws IOException {
        try (SocketChannel client = SocketChannel.open(HOST_ADDRESS);
             Scanner in = new Scanner(System.in)) {

            readName(in);
            sendName(client);

            Thread readThread = new Thread(() -> {
                printMessages(client);
            });
            readThread.setDaemon(true);
            readThread.start();

            String line;
            while (!(line = in.nextLine()).equals("exit")) {
                sendMessage(client, new Message(TEXT, line));
            }

            readThread.stop();
            in.close();
            client.close();
            System.exit(0);
        }
    }

    private void sendName(SocketChannel client) throws IOException {
        sendMessage(client, new Message(USER_NAME, CLIENT_NAME));
    }

    private void readName(Scanner in) {
        System.out.println("Enter your name: ");
        String name = in.nextLine();
        if (isNotBlank(name)) {
            CLIENT_NAME = name;
        }
    }

    private void sendMessage(SocketChannel client, Message message) throws IOException {
        byte[] bytes = serialize(message);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        client.write(buffer);
        buffer.clear();
    }

    private void printMessages(SocketChannel client) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (client.isConnected()) {
            try {
                client.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Message message = (Message) deserialize(buffer.array());

            System.out.println(message.getText());
            buffer.clear();
        }
    }
}
