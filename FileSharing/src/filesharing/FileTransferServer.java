package filesharing;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.*;

public class FileTransferServer {
    private final Path uploadDir;
    private final ConcurrentHashMap<String, FileMetadata> files = new ConcurrentHashMap<>();
    private final ExecutorService workers = Executors.newFixedThreadPool(16);
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public FileTransferServer(Path uploadDir) throws IOException {
        this.uploadDir = uploadDir.toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public void start(int port) throws IOException {
        cleaner.scheduleAtFixedRate(this::deleteExpiredFiles, 1, 1, TimeUnit.SECONDS);
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("File Server running on port " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = server.accept();
                try {
                    workers.execute(() -> handle(socket));
                } catch (RejectedExecutionException ex) {
                    try { socket.close(); } catch (IOException ignored) {}
                }
            }
        } finally {
            cleaner.shutdownNow();
            workers.shutdownNow();
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket;
             DataInputStream in = new DataInputStream(new BufferedInputStream(s.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(s.getOutputStream()))) {
            s.setSoTimeout(FileProtocol.READ_TIMEOUT_MS);
            String command = in.readUTF();
            if (FileProtocol.UPLOAD.equals(command)) upload(in, out);
            else if (FileProtocol.DOWNLOAD.equals(command)) download(in, out);
            else sendError(out, "Unknown command");
        } catch (IOException ex) {
            System.err.println("Client disconnected/transfer failed: " + ex.getMessage());
        }
    }

    private void upload(DataInputStream in, DataOutputStream out) throws IOException {
        String originalName = in.readUTF();
        long size = in.readLong();
        long ttlSeconds = in.readLong();
        if (originalName.trim().isEmpty() || originalName.length() > 240
                || originalName.contains("/") || originalName.contains("\\")
                || originalName.equals(".") || originalName.equals("..")
                || size < 0 || size > FileProtocol.MAX_FILE_SIZE
                || ttlSeconds <= 0 || ttlSeconds > FileProtocol.MAX_TTL_SECONDS) {
            sendError(out, "Invalid filename, size or TTL");
            return;
        }
        out.writeUTF(FileProtocol.OK); // ready to receive
        out.flush();
        String id = UUID.randomUUID().toString();
        Path part = uploadDir.resolve(id + ".part");
        Path target = uploadDir.resolve(id + ".bin");
        MessageDigest digest = sha256();
        try {
            try (OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(part, StandardOpenOption.CREATE_NEW))) {
                byte[] buffer = new byte[FileProtocol.BUFFER_SIZE];
                long remaining = size;
                while (remaining > 0) {
                    int count = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (count < 0) throw new EOFException("Upload incomplete");
                    fileOut.write(buffer, 0, count);
                    digest.update(buffer, 0, count);
                    remaining -= count;
                }
            }
            String actualHash = toHex(digest.digest());
            String expectedHash = in.readUTF();
            if (!actualHash.equalsIgnoreCase(expectedHash)) {
                sendError(out, "SHA-256 mismatch");
                return;
            }
            Files.move(part, target);
            long now = System.currentTimeMillis();
            long expiresAt = now + ttlSeconds * 1000L;
            files.put(id, new FileMetadata(id, originalName, target, size, expiresAt, actualHash));
            out.writeUTF(FileProtocol.OK);
            out.writeUTF(id);
            out.writeLong(expiresAt);
            out.flush();
            System.out.println("Uploaded " + originalName + " -> " + id);
        } finally {
            Files.deleteIfExists(part);
        }
    }

    private void download(DataInputStream in, DataOutputStream out) throws IOException {
        String id = in.readUTF();
        FileMetadata meta = files.get(id);
        if (meta == null || meta.isExpired()) {
            sendError(out, "File not found or expired");
            return;
        }
        // Lock metadata during streaming so cleaner cannot delete this file mid-transfer.
        synchronized (meta) {
            if (files.get(id) != meta || meta.isExpired() || !Files.isRegularFile(meta.getStoredPath())) {
                sendError(out, "File not found or expired");
                return;
            }
            out.writeUTF(FileProtocol.OK);
            out.writeUTF(meta.getOriginalName());
            out.writeLong(meta.getSize());
            out.writeUTF(meta.getSha256());
            try (InputStream fileIn = new BufferedInputStream(Files.newInputStream(meta.getStoredPath()))) {
                byte[] buffer = new byte[FileProtocol.BUFFER_SIZE];
                int count;
                while ((count = fileIn.read(buffer)) != -1) out.write(buffer, 0, count);
            }
            out.flush();
        }
    }

    public boolean deleteFile(String id) {
        FileMetadata meta = files.get(id);
        if (meta == null) return false;
        synchronized (meta) {
            if (files.get(id) != meta) return false;
            try {
                Files.deleteIfExists(meta.getStoredPath());
                files.remove(id, meta);
                return true;
            } catch (IOException ex) {
                System.err.println("Delete failed " + id + ": " + ex.getMessage());
                return false;
            }
        }
    }

    private void deleteExpiredFiles() {
        for (FileMetadata meta : files.values()) {
            if (meta.isExpired() && deleteFile(meta.getId()))
                System.out.println("Expired file deleted: " + meta.getId());
        }
    }

    private static void sendError(DataOutputStream out, String message) throws IOException {
        out.writeUTF(FileProtocol.ERROR);
        out.writeUTF(message);
        out.flush();
    }

    static MessageDigest sha256() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) sb.append(String.format("%02x", b & 0xff));
        return sb.toString();
    }

    public static void main(String[] args) throws IOException {
        new FileTransferServer(Paths.get("uploads")).start(FileProtocol.PORT);
    }
}
