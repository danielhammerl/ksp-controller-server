package de.danielhammerl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;

public class PersistentTcpServer {
    private final int port;
    private volatile boolean running = true;

    private Thread serverThread;
    private final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private DataListener listener;

    public interface DataListener {
        void onDataReceived(ClientHandler client, byte[] data, int length);
    }

    public PersistentTcpServer(int port, DataListener listener) {
        this.port = port;
        this.listener = listener;

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

    public class ClientHandler {
        private Socket socket;
        private InputStream in;
        private OutputStream out;
        private Thread thread;
        private volatile boolean connected = true;

        public ClientHandler(Socket socket) throws IOException {
            this.socket = socket;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
        }

        public void start() {
            thread = new Thread(this::runLoop, "ClientThread-" + socket.getRemoteSocketAddress());
            thread.setDaemon(true);
            thread.start();
        }

        private void runLoop() {
            byte[] buffer = new byte[1024];
            try {
                while (connected && !socket.isClosed()) {
                    int read = in.read(buffer);
                    if (read == -1) break;
                    if (listener != null) {
                        listener.onDataReceived(this, buffer, read);
                    }
                }
            } catch (IOException e) {
                // Verbindung verloren
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
