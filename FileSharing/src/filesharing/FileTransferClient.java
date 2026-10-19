package filesharing;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.security.MessageDigest;

public class FileTransferClient {
    private final String host;
    private final int port;

    public FileTransferClient(String host, int port) {
        this.host = host;
        this.port = port;
    }
    private Socket connect() throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), FileProtocol.CONNECT_TIMEOUT_MS);
        socket.setSoTimeout(FileProtocol.READ_TIMEOUT_MS);
        return socket;
    }

    public String upload(Path source, long ttlSeconds) throws IOException {
        if (!Files.isRegularFile(source)) throw new FileNotFoundException(source.toString());
        long size = Files.size(source);
        if (size > FileProtocol.MAX_FILE_SIZE || ttlSeconds <= 0 || ttlSeconds > FileProtocol.MAX_TTL_SECONDS)
            throw new IllegalArgumentException("Invalid file size or TTL");
        String name = source.getFileName().toString();
        if (name.length() > 240 || name.contains("/") || name.contains("\\"))
            throw new IllegalArgumentException("Invalid filename");
        try (Socket socket = connect();
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            out.writeUTF(FileProtocol.UPLOAD);
            out.writeUTF(name);
            out.writeLong(size);
            out.writeLong(ttlSeconds);
            out.flush();
            checkResponse(in);
            MessageDigest digest = FileTransferServer.sha256();
            try (InputStream fileIn = new BufferedInputStream(Files.newInputStream(source))) {
                byte[] buffer = new byte[FileProtocol.BUFFER_SIZE];
                long remaining = size;
                while (remaining > 0) {
                    int count = fileIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                    if (count < 0) throw new EOFException("Local file changed during upload");
                    out.write(buffer, 0, count);
                    digest.update(buffer, 0, count);
                    remaining -= count;
                }
            }
            out.writeUTF(FileTransferServer.toHex(digest.digest()));
            out.flush();
            checkResponse(in);
            String id = in.readUTF();
            long expiresAt = in.readLong();
            System.out.println("Upload OK; expires at (epoch ms): " + expiresAt);
            return id;
        }
    }

    public Path download(String fileId, Path directory) throws IOException {
        Files.createDirectories(directory);
        try (Socket socket = connect();
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
            out.writeUTF(FileProtocol.DOWNLOAD);
            out.writeUTF(fileId);
            out.flush();
            checkResponse(in);
            String name = in.readUTF();
            long size = in.readLong();
            String expectedHash = in.readUTF();
            if (size < 0 || size > FileProtocol.MAX_FILE_SIZE || name.isEmpty()
                    || name.contains("/") || name.contains("\\") || name.equals(".") || name.equals(".."))
                throw new IOException("Invalid download metadata");
            Path part = Files.createTempFile(directory, "download-", ".part");
            try {
                MessageDigest digest = FileTransferServer.sha256();
                try (OutputStream fileOut = new BufferedOutputStream(Files.newOutputStream(part))) {
                    byte[] buffer = new byte[FileProtocol.BUFFER_SIZE];
                    long remaining = size;
                    while (remaining > 0) {
                        int count = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                        if (count < 0) throw new EOFException("Download incomplete");
                        fileOut.write(buffer, 0, count);
                        digest.update(buffer, 0, count);
                        remaining -= count;
                    }
                }
                if (!FileTransferServer.toHex(digest.digest()).equalsIgnoreCase(expectedHash))
                    throw new IOException("Downloaded file SHA-256 mismatch");
                Path destination = uniquePath(directory, name);
                Files.move(part, destination);
                return destination;
            } finally {
                Files.deleteIfExists(part);
            }
        }
    }

    private static Path uniquePath(Path dir, String name) throws IOException {
        Path candidate = dir.resolve(name);
        if (!Files.exists(candidate)) return candidate;
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; i < 10000; i++) {
            candidate = dir.resolve(base + " (" + i + ")" + ext);
            if (!Files.exists(candidate)) return candidate;
        }
        throw new IOException("No free download filename");
    }

    private static void checkResponse(DataInputStream in) throws IOException {
        String status = in.readUTF();
        if (FileProtocol.ERROR.equals(status)) throw new IOException("Server: " + in.readUTF());
        if (!FileProtocol.OK.equals(status)) throw new IOException("Unexpected response: " + status);
    }
}
