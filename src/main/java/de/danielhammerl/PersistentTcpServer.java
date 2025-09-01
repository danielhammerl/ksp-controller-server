package de.danielhammerl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

// enormous shitload of code
public class PersistentTcpServer {
    private final int BUFFER_SIZE = 512;
    private final int port;
    private volatile boolean running = true;
    private final Thread serverThread;
    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final LinkedBlockingQueue<DataPacket> dataQueue = new LinkedBlockingQueue<>();

    public static class DataPacket {
        public final ClientHandler client;
        public final byte[] data;
        public final int length;

        public DataPacket(ClientHandler client, byte[] data, int length) {
            this.client = client;
            this.data = data;
            this.length = length;
        }
    }

    public PersistentTcpServer(int port) {
        this.port = port;
        serverThread = new Thread(this::runServer, "TcpServerThread");
        serverThread.setDaemon(true);
        serverThread.start();
        System.out.println("Server listening on port " + port);
    }

    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientHandler client = new ClientHandler(socket);
                    clients.add(client);
                    client.start();
                    System.out.println("Client connected: " + socket.getRemoteSocketAddress());
                } catch (IOException e) {
                    if (running) e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean getConnected() {
        return clients.stream().anyMatch(ClientHandler::isConnected);
    }

    public void stop() {
        running = false;
        for (ClientHandler c : clients) {
            c.stop();
        }
    }

    public DataPacket waitForData(int timeoutMillis) throws InterruptedException {
        return dataQueue.poll(timeoutMillis, TimeUnit.MILLISECONDS);
    }

    public void sendData(byte[] data) throws IOException {
        for (ClientHandler c : clients) {
            if (c.isConnected()) {
                c.send(data);
            }
        }
    }

    public class ClientHandler {
        private final Socket socket;
        private final InputStream in;
        private final OutputStream out;
        private volatile boolean connected = true;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        public void start() {
            Thread thread = new Thread(this::runLoop, "ClientThread-" + socket.getRemoteSocketAddress());
            thread.setDaemon(true);
            thread.start();
        }

        private void runLoop() {
            byte[] buffer = new byte[BUFFER_SIZE];
            try {
                socket.setSoTimeout(2000);
                while (connected && !socket.isClosed()) {
                    int read = in.read(buffer);
                    if (read == -1) break;
                    dataQueue.offer(new DataPacket(this, buffer.clone(), read));
                }
            } catch (IOException e) {
                // Connection lost or timeout
            } finally {
                connected = false;
                clients.remove(this);
                try { socket.close(); } catch (IOException ignored) {}
                System.out.println("Client disconnected: " + socket.getRemoteSocketAddress());
            }
        }

        public synchronized void send(byte[] data) throws IOException {
            if (connected && out != null) {
                out.write(data);
                out.flush();
            }
        }

        public boolean isConnected() {
            return connected;
        }

        public void stop() {
            connected = false;
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}