/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.instant_messaging;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
/**
 *
 * @author Cafe Tam Loan
 */
public class ServerMain {
    private static final int PORT = 8888; // Cổng kết nối của Server

    public static void main(String[] args) {
        // Tạo ThreadPool quản lý tối đa nhiều luồng đồng thời cho nhiều Client
        ExecutorService threadPool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("========================================");
            System.out.println(" SERVER CHAT ĐANG KHỞI ĐỘNG TẠI PORT " + PORT);
            System.out.println("========================================");

            while (true) {
                // Chờ và chấp nhận kết nối từ Client mới
                Socket clientSocket = serverSocket.accept();
                
                // Giao client này cho ClientHandler xử lý độc lập trên một luồng riêng
                ClientHandler handler = new ClientHandler(clientSocket);
                threadPool.execute(handler);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khởi động Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
