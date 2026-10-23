
package multiclientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private boolean registered = false;

    // Constructor duy nhat
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream(),
                            StandardCharsets.UTF_8));

            out = new PrintWriter(
                    socket.getOutputStream(), true);

            out.println("Nhap ten cua ban:");

            username = in.readLine();

            if (username == null || username.trim().isEmpty()) {
                return;
            }

            username = username.trim();

            // Kiem tra ten dang nhap
            if (!ServerMain.addClient(username, this)) {
                out.println("TEN_DA_TON_TAI");
                return;
            }

            registered = true;

            out.println("Ket noi thanh cong! Go /quit de thoat.");

            System.out.println(username + " da tham gia.");
            ServerMain.showOnlineClients();

            ServerMain.broadcast(
                    "[SERVER] " + username
                    + " da tham gia phong chat.");

            String message;

            // Nhan tin nhan lien tuc
            while ((message = in.readLine()) != null) {

                if (message.equalsIgnoreCase("/quit")) {
                    break;
                }

                if (!message.trim().isEmpty()) {
                    String fullMessage = username + ": " + message;

                    System.out.println(fullMessage);
                    ServerMain.broadcast(fullMessage);
                }
            }

        } catch (IOException e) {
            System.out.println("Client "
                    + username + " mat ket noi.");
        } finally {
            // Don dep Client khi thoat hoac gap loi
            if (registered) {
                ServerMain.removeClient(username, this);

                ServerMain.broadcast(
                        "[SERVER] " + username
                        + " da roi phong chat.");

                System.out.println(username + " da ngat ket noi.");
                ServerMain.showOnlineClients();
            }

            try {
                socket.close();
            } catch (IOException e) {
                System.err.println(
                        "Loi dong Socket: " + e.getMessage());
            }
        }
    }

    // Gui tin nhan den Client nay
    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }
}
