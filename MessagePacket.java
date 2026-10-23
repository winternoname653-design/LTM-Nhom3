/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.instant_messaging;

/**
 *
 * @author Cafe Tam Loan
 */
public class MessagePacket {
    private String type;     // Loại lệnh: MSG, BROADCAST, PRIVATE, DESTROY_SIGNAL
    private String sender;   // Người gửi
    private String receiver; // Người nhận (ALL nếu là broadcast)
    private String content;  // Nội dung tin nhắn
    private long ttl;        // Thời gian tự hủy tính bằng giây (Time-To-Live)

    public MessagePacket(String type, String sender, String receiver, String content, long ttl) {
        this.type = type;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.ttl = ttl;
    }

    // Chuyển đổi đối tượng thành chuỗi để gửi qua Socket (Phân cách bởi dấu |)
    public String serialize() {
        return type + "|" + sender + "|" + receiver + "|" + content + "|" + ttl;
    }

    // Giải mã chuỗi nhận được từ Socket thành đối tượng MessagePacket
    public static MessagePacket deserialize(String rawData) {
        String[] parts = rawData.split("\\|", -5); // Giữ lại các chuỗi rỗng nếu có
        if (parts.length >= 5) {
            String type = parts[0];
            String sender = parts[1];
            String receiver = parts[2];
            String content = parts[3];
            long ttl = 0;
            try {
                ttl = Long.parseLong(parts[4]);
            } catch (NumberFormatException e) {
                ttl = 0;
            }
            return new MessagePacket(type, sender, receiver, content, ttl);
        }
        return null;
    }

    // Getters và Setters
    public String getType() { return type; }
    public String getSender() { return sender; }
    public String getReceiver() { return receiver; }
    public String getContent() { return content; }
    public long getTtl() { return ttl; }
}
