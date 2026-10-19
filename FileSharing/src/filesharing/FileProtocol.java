package filesharing;

public final class FileProtocol {
    private FileProtocol() {}
    public static final int PORT = 5050;
    public static final int BUFFER_SIZE = 8192;
    public static final int CONNECT_TIMEOUT_MS = 10000;
    public static final int READ_TIMEOUT_MS = 30000;
    public static final long MAX_FILE_SIZE = 1024L * 1024 * 1024; // 1 GiB demo limit
    public static final long MAX_TTL_SECONDS = 7L * 24 * 3600;
    public static final String UPLOAD = "UPLOAD";
    public static final String DOWNLOAD = "DOWNLOAD";
    public static final String OK = "OK";
    public static final String ERROR = "ERROR";
}
