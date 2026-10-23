/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.instant_messaging;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
/**
 *
 * @author Cafe Tam Loan
 */
public class ClientHandler implements Runnable {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String clientUsername;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            // Mở luồng đọc và ghi dữ liệu qua Socket
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(socket.getOutputStream(), true);

            String clientMessage;
            // Vòng lặp liên tục chờ đọc dữ liệu từ Client gửi lên
            while ((clientMessage = reader.readLine()) != null) {
                // Giải mã gói tin
                MessagePacket packet = MessagePacket.deserialize(clientMessage);
                if (packet == null) continue;

                // Xử lý theo từng loại lệnh yêu cầu
                switch (packet.getType().toUpperCase()) {
                    case "CONNECT":
                        this.clientUsername = packet.getSender();
                        MessageRouter.registerClient(this.clientUsername, writer);
                        break;

                    case "BROADCAST":
                    case "PRIVATE":
                    case "MSG":
                        // Gọi bộ định tuyến của Thành viên 2 để chuyển tin nhắn đi
                        MessageRouter.routeMessage(packet);
                        break;

                    case "DISCONNECT":
                        return; // Thoát luồng để ngắt kết nối

                    default:
                        System.out.println("[WARNING] Lệnh không hợp lệ: " + packet.getType());
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Mất kết nối với client: " + clientUsername);
        } finally {
            // Dọn dẹp tài nguyên khi Client ngắt kết nối hoặc gặp lỗi
            cleanup();
        }
    }

    private void cleanup() {
        MessageRouter.removeClient(clientUsername);
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
