
package multiclientserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientTest {

    public static void main(String[] args) {

        try (
            Socket socket = new Socket("localhost", 5000);

            BufferedReader in = new BufferedReader(
                new InputStreamReader(
                    socket.getInputStream(),
                    StandardCharsets.UTF_8
                )
            );

            PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true
            );

            BufferedReader keyboard = new BufferedReader(
                new InputStreamReader(System.in)
            )
        ) {
            System.out.println("Da ket noi den Server!");

            // Nhan yeu cau nhap ten
            System.out.println(in.readLine());

            String username = keyboard.readLine();
            out.println(username);

            // Nhan ket qua dang nhap
            String response = in.readLine();
            System.out.println(response);

            // Neu ten bi trung thi ket thuc Client
            if ("TEN_DA_TON_TAI".equals(response)) {
                System.out.println("Vui long chon ten khac.");
                return;
            }

            // Luong rieng de nhan tin nhan tu Server
            Thread receiver = new Thread(() -> {
                try {
                    String message;

                    while ((message = in.readLine()) != null) {
                        System.out.println(message);
                    }

                } catch (IOException e) {
                    System.out.println("Da mat ket noi Server.");
                }
            });

            receiver.setDaemon(true);
            receiver.start();

            // Nhap va gui tin nhan
            System.out.println(
                "Nhap tin nhan de gui. Go /quit de thoat."
            );

            String message;

            while ((message = keyboard.readLine()) != null) {
                out.println(message);

                if (message.equalsIgnoreCase("/quit")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println(
                "Khong the ket noi Server: " + e.getMessage()
            );
            System.out.println(
                "Hay kiem tra Server co dang chay o cong 5000 khong."
            );
        }
    }
}
