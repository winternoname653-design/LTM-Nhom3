
package multiclientserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {

    public static final int PORT = 5000;

    // Danh sach Client dang online
    public static final ConcurrentHashMap<String, ClientHandler> clients
            = new ConcurrentHashMap<>();

    // Quan ly cac luong xu ly Client
    private static final ExecutorService threadPool
            = Executors.newFixedThreadPool(50);

    public static void main(String[] args) {
        System.out.println("==================================");
        System.out.println("     MULTI-CLIENT CHAT SERVER");
        System.out.println("==================================");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server dang chay tai cong: " + PORT);
            System.out.println("Dang cho Client ket noi...\n");

            while (!serverSocket.isClosed()) {
                Socket socket = serverSocket.accept();

                System.out.println("Client moi ket noi: "
                        + socket.getRemoteSocketAddress());

                ClientHandler handler = new ClientHandler(socket);
                threadPool.execute(handler);
            }

        } catch (IOException e) {
            System.err.println("Loi Server: " + e.getMessage());
        } finally {
            threadPool.shutdown();
            System.out.println("Server da dung.");
        }
    }

    // Gui tin nhan den tat ca Client
    public static void broadcast(String message) {
        for (ClientHandler client : clients.values()) {
            client.sendMessage(message);
        }
    }

    // Them Client, khong cho phep trung ten
    public static boolean addClient(
            String username, ClientHandler handler) {
        return clients.putIfAbsent(username, handler) == null;
    }

    // Xoa Client khi ngat ket noi
    public static void removeClient(
            String username, ClientHandler handler) {
        if (username != null) {
            clients.remove(username, handler);
        }
    }

    // Hien thi danh sach Client online
    public static void showOnlineClients() {
        System.out.println("So Client dang online: "
                + clients.size());
        System.out.println("Danh sach: " + clients.keySet());
    }
}
