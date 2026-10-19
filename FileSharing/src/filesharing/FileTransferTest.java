package filesharing;

import java.nio.file.*;
import java.security.MessageDigest;
import java.util.Scanner;

public class FileTransferTest {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Server IP [127.0.0.1]: ");
        String host = scanner.nextLine().trim();
        if (host.isEmpty()) host = "127.0.0.1";
        FileTransferClient client = new FileTransferClient(host, FileProtocol.PORT);
        while (true) {
            System.out.println("\n1. Upload  2. Download  0. Exit");
            System.out.print("Choose: ");
            String choice = scanner.nextLine().trim();
            try {
                switch (choice) {
                    case "1":
                        System.out.print("File path: ");
                        Path source = Paths.get(scanner.nextLine().trim().replace("\"", ""));
                        System.out.print("TTL (seconds): ");
                        long ttl = Long.parseLong(scanner.nextLine().trim());
                        String id = client.upload(source, ttl);
                        System.out.println("SUCCESS - FileID: " + id);
                        break;
                    case "2":
                        System.out.print("FileID: ");
                        String fileId = scanner.nextLine().trim();
                        Path saved = client.download(fileId, Paths.get("downloads"));
                        System.out.println("SUCCESS - Saved to: " + saved.toAbsolutePath());
                        break;
                    case "0": return;
                    default: System.out.println("Invalid choice");
                }
            } catch (Exception ex) {
                System.err.println("FAILED: " + ex.getMessage());
            }
        }
    }
}