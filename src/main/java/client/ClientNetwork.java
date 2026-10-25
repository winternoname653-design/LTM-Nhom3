/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client;

/**
 *
 * @author ngomi
 */

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientNetwork {
    public interface Listener {
        void onConnected();
        void onDisconnected(String reason);
        void onMessage(String sender, String message, int ttl);
        void onExpired(String id, String name);
        void onInfo(String message);
        void onError(String message);
    }

    private volatile Socket socket;
    private volatile BufferedReader reader;
    private volatile PrintWriter writer;
    private volatile boolean connected;

    private final Listener listener;
    private final ExecutorService sendExecutor =
            Executors.newSingleThreadExecutor();

    public ClientNetwork(Listener listener) {
        this.listener = listener;
    }

    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }

    public void connect(String host, int port) {
        sendExecutor.execute(() -> {
            try {
                disconnectInternal();

                Socket newSocket = new Socket(host, port);
                newSocket.setKeepAlive(true);

                socket = newSocket;
                reader = new BufferedReader(
                        new InputStreamReader(
                                newSocket.getInputStream(),
                                StandardCharsets.UTF_8
                        )
                );
                writer = new PrintWriter(
                        new OutputStreamWriter(
                                newSocket.getOutputStream(),
                                StandardCharsets.UTF_8
                        ),
                        true
                );

                connected = true;
                listener.onConnected();

                // Luồng rieng chuyen nhan du lieu.
                Thread receiveThread = new Thread(
                        this::receiveLoop,
                        "Client-Receive"
                );
                receiveThread.setDaemon(true);
                receiveThread.start();

            } catch (IOException ex) {
                disconnectInternal();
                listener.onError(
                        "Khong tha ket noi: " + ex.getMessage()
                );
            }
        });
    }

    private void receiveLoop() {
        String reason = "Server da dong ket noi.";

        try {
            String line;

            while (connected
                    && (line = reader.readLine()) != null) {
                handleServerLine(line);
            }

        } catch (IOException ex) {
            reason = "Lỗi ket noi: " + ex.getMessage();
        } finally {
            boolean wasConnected = connected;
            disconnectInternal();

            if (wasConnected) {
                listener.onDisconnected(reason);
            }
        }
    }

    private void handleServerLine(String line) {
        String[] parts = line.split("\\|", 4);

        try {
            switch (parts[0]) {
                case "MSG":
                    if (parts.length != 4) {
                        listener.onInfo("Goi tin khong hop le.");
                        return;
                    }

                    String message = decode(parts[3]);
                    int ttl = Integer.parseInt(parts[2]);

                    listener.onMessage(parts[1], message, ttl);
                    break;

                case "EXPIRED":
                    String[] expired = line.split("\\|", 3);

                    if (expired.length == 3) {
                        listener.onExpired(
                                expired[1],
                                decode(expired[2])
                        );
                    }
                    break;

                case "INFO":
                    listener.onInfo(
                            parts.length > 1
                                    ? decode(parts[1]) : line
                    );
                    break;

                default:
                    listener.onInfo("[Server] " + line);
            }
        } catch (Exception ex) {
            listener.onError(
                    "Khong doc duoc du lieu Server: "
                            + ex.getMessage()
            );
        }
    }

    public void sendMessage(String message, int ttl) {
        String encoded = encode(message);
        sendLine("MSG|" + ttl + "|" + encoded);
    }

    public void sendFile(Path path, int ttl) {
        sendExecutor.execute(() -> {
            try {
                if (!isConnected()) {
                    listener.onError("Chua ket noi Server.");
                    return;
                }

                // Gioi han ban demo: 2 MB.
                if (Files.size(path) > 2 * 1024 * 1024) {
                    listener.onError(
                            "Tep qua lon. Ban demo gioi han 2 MB."
                    );
                    return;
                }

                byte[] bytes = Files.readAllBytes(path);
                String fileName = encode(
                        path.getFileName().toString()
                );
                String data = Base64.getEncoder()
                        .encodeToString(bytes);

                sendLine(
                        "FILE|" + ttl + "|" + fileName + "|" + data
                );

                listener.onInfo(
                        "Da gui yeu cau gui tep: "
                                + path.getFileName()
                );

            } catch (IOException ex) {
                listener.onError(
                        "Khong doc duoc tep: " + ex.getMessage()
                );
            }
        });
    }

    private void sendLine(String line) {
        sendExecutor.execute(() -> {
            if (!isConnected() || writer == null) {
                listener.onError("Chua ket noi Server.");
                return;
            }

            writer.println(line);

            if (writer.checkError()) {
                listener.onError("Gui du lieu that bai.");
            }
        });
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(
                value.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String decode(String value) {
        return new String(
                Base64.getDecoder().decode(value),
                StandardCharsets.UTF_8
        );
    }

    public void disconnect() {
        disconnectInternal();
    }

    private synchronized void disconnectInternal() {
        connected = false;

        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }

        socket = null;
        reader = null;
        writer = null;
    }

    public void shutdown() {
        disconnect();
        sendExecutor.shutdownNow();
    }
}
