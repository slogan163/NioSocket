package console;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;

public class ClientTerminal {

    public static void main(String[] args) throws IOException {
        InetSocketAddress hostAddress = new InetSocketAddress("localhost", 8090);
        Scanner in = new Scanner(System.in);

        try (SocketChannel client = SocketChannel.open(hostAddress)) {
            Thread readThread = new Thread(() -> {
                printMessages(client);
            });
            readThread.setDaemon(true);
            readThread.start();

            String line;
            while (!(line = in.nextLine()).equals("exit")) {
                byte[] message = line.getBytes();
                ByteBuffer buffer = ByteBuffer.wrap(message);
                client.write(buffer);
                System.out.println("Client send: " + message);
                buffer.clear();
            }
        }
    }

    private static void printMessages(SocketChannel client) {
        ByteBuffer buffer = ByteBuffer.allocate(1024);

        while (client.isOpen()){
            try {
                client.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Buffer: " + new String(buffer.array()));
        }
    }
}
