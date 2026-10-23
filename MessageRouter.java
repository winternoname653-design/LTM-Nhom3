/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.instant_messaging;
import java.io.PrintWriter;
import java.util.concurrent.ConcurrentHashMap;
/**
 *
 * @author Cafe Tam Loan
 */
public class MessageRouter {
    // Lưu trữ danh sách các client online: <Username, PrintWriter để gửi dữ liệu>
    private static final ConcurrentHashMap<String, PrintWriter> onlineClients = new ConcurrentHashMap<>();

    // Đăng ký client khi kết nối thành công
    public static void registerClient(String username, PrintWriter writer) {
        onlineClients.put(username, writer);
    }

    // Xóa client khi ngắt kết nối
    public static void removeClient(String username) {
        onlineClients.remove(username);
    }

    // Định tuyến tin nhắn đến người nhận
    public static void routeMessage(MessagePacket packet) {
        String rawPacket = packet.serialize();

        if ("BROADCAST".equalsIgnoreCase(packet.getReceiver()) || "ALL".equalsIgnoreCase(packet.getReceiver())) {
            // Gửi cho tất cả các client đang online
            for (PrintWriter writer : onlineClients.values()) {
                writer.println(rawPacket);
                writer.flush();
            }
        } else {
            // Gửi riêng cho một client (Private Chat)
            PrintWriter targetWriter = onlineClients.get(packet.getReceiver());
            if (targetWriter != null) {
                targetWriter.println(rawPacket);
                targetWriter.flush();
            }
            
            // Gửi lại cho chính người gửi để hiển thị trên giao diện của họ
            PrintWriter senderWriter = onlineClients.get(packet.getSender());
            if (senderWriter != null) {
                senderWriter.println(rawPacket);
                senderWriter.flush();
            }
        }
    }
}
