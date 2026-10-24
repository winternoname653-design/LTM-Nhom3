/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.selfdestruct;

/**
 *
 * @author klerl
 */
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.*;
public class SelfDestructEngine {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // Tham chiếu đến kho lưu trữ (ConcurrentHashMap) của Server
    private final ConcurrentHashMap<String, FileMetadata> storage;

    public SelfDestructEngine(ConcurrentHashMap<String, FileMetadata> storage) {
        this.storage = storage;
    }

    /**
     * Khởi động bộ máy tự hủy, quét mỗi giây một lần
     */
    public void start() {
        // Quét định kỳ để kiểm tra file hết hạn dựa trên logic từ FileTransferServer
        scheduler.scheduleAtFixedRate(this.scanAndDestroyExpiredFiles, 1, 1, TimeUnit.SECONDS);
        System.out.println("[SelfDestructEngine] Đã khởi động cơ chế tự hủy ngầm thành công.");
    }

    /**
     * Tác vụ quét và tiêu hủy các file đã hết hạn TTL
     */
    private final Runnable scanAndDestroyExpiredFiles = () -> {
        for (FileMetadata meta : storage.values()) {
            // Kiểm tra trạng thái hết hạn thông qua phương thức isExpired()
            if (meta.isExpired()) {
                destruct(meta);
            }
        }
    };

    /**
     * Thực hiện quy trình hủy: Xóa file vật lý và dọn dẹp bộ nhớ đệm
     */
    public boolean destruct(FileMetadata meta) {
        // Sử dụng synchronized để đảm bảo an toàn đa luồng, tránh xung đột khi client đang tải[cite: 4]
        synchronized (meta) {
            if (storage.get(meta.getId()) != meta) return false;
            try {
                // 1. Xóa file vật lý khỏi thư mục lưu trữ trên Server[cite: 4]
                Files.deleteIfExists(meta.getStoredPath());

                // 2. Xóa dữ liệu khỏi bộ nhớ đệm / Map của Server[cite: 4]
                storage.remove(meta.getId(), meta);

                System.out.println("[SelfDestructEngine] ĐÃ TỰ HỦY THÀNH CÔNG - ID: " + meta.getId() 
                    + " | Tên file: " + meta.getOriginalName());
                return true;
            } catch (IOException ex) {
                System.err.println("[SelfDestructEngine] Lỗi khi xóa file " + meta.getId() + ": " + ex.getMessage());
                return false;
            }
        }
    }

    /**
     * Dừng engine khi tắt Server
     */
    public void shutdown() {
        scheduler.shutdownNow();
        System.out.println("[SelfDestructEngine] Đã dừng cơ chế tự hủy.");
    }
}
