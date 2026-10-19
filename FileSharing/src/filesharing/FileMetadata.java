package filesharing;

import java.nio.file.Path;

public final class FileMetadata {
    private final String id;
    private final String originalName;
    private final Path storedPath;
    private final long size;
    private final long expiresAt;
    private final String sha256;

    public FileMetadata(String id, String originalName, Path storedPath,
                        long size, long expiresAt, String sha256) {
        this.id = id;
        this.originalName = originalName;
        this.storedPath = storedPath;
        this.size = size;
        this.expiresAt = expiresAt;
        this.sha256 = sha256;
    }
    public String getId() { return id; }
    public String getOriginalName() { return originalName; }
    public Path getStoredPath() { return storedPath; }
    public long getSize() { return size; }
    public long getExpiresAt() { return expiresAt; }
    public String getSha256() { return sha256; }
    public boolean isExpired() { return System.currentTimeMillis() >= expiresAt; }
}
