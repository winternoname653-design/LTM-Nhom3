/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package client;

/**
 *
 * @author ngomi
 */
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ClientApp extends JFrame
        implements ClientNetwork.Listener {
    private JTextField txtHost;
    private JTextField txtPort;
    private JTextField txtMessage;
    private JComboBox<String> cboTtl;
    private JTextArea txtChat;
    private JLabel lblStatus;

    private ClientNetwork network;
    private final List<ExpiringItem> expiringItems =
            new ArrayList<>();

    private static class ExpiringItem {
        String description;
        long expiresAt;

        ExpiringItem(String description, int ttl) {
            this.description = description;
            this.expiresAt = Instant.now().getEpochSecond() + ttl;
        }
    }

    public ClientApp() {
        network = new ClientNetwork(this);

        setTitle("Self-Destructing Message - Client");
        setSize(780, 540);
        setMinimumSize(new Dimension(650, 450));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();

        // Cập nhật dồng hồ dem nguoc mỗi giay.
        Timer timer = new Timer(1000, e -> updateCountdowns());
        timer.start();

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(
                    java.awt.event.WindowEvent e) {
                network.shutdown();
                timer.stop();
                dispose();
            }
        });
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBorder(
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        );

        // Thanh ket nối
        JPanel connection = new JPanel(new FlowLayout(FlowLayout.LEFT));

        txtHost = new JTextField("127.0.0.1", 12);
        txtPort = new JTextField("8080", 5);

        JButton btnConnect = new JButton("Ket noi");
        JButton btnDisconnect = new JButton("Ngat ket noi");

        lblStatus = new JLabel("Chua ket noi");

        connection.add(new JLabel("IP Server:"));
        connection.add(txtHost);
        connection.add(new JLabel("Port:"));
        connection.add(txtPort);
        connection.add(btnConnect);
        connection.add(btnDisconnect);

        // Khung chat
        txtChat = new JTextArea();
        txtChat.setEditable(false);
        txtChat.setLineWrap(true);
        txtChat.setWrapStyleWord(true);
        txtChat.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JScrollPane chatScroll = new JScrollPane(txtChat);

        // Thanh nhập nội dung
        JPanel compose = new JPanel(new BorderLayout(6, 6));

        txtMessage = new JTextField();
        txtMessage.setFont(new Font("SansSerif", Font.PLAIN, 14));

        cboTtl = new JComboBox<>(new String[]{
            "10 giay",
            "30 giay",
            "1 phut",
            "5 phut"
        });
        cboTtl.setSelectedIndex(1);

        JButton btnSend = new JButton("Gui tin nhan");
        JButton btnFile = new JButton("Chon và gui tep");

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(new JLabel("Tu huy sau:"));
        actions.add(cboTtl);
        actions.add(btnFile);
        actions.add(btnSend);

        compose.add(txtMessage, BorderLayout.CENTER);
        compose.add(actions, BorderLayout.SOUTH);

        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(compose, BorderLayout.CENTER);
        bottom.add(lblStatus, BorderLayout.SOUTH);

        root.add(connection, BorderLayout.NORTH);
        root.add(chatScroll, BorderLayout.CENTER);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);

        btnConnect.addActionListener(e -> connect());
        btnDisconnect.addActionListener(e -> disconnect());
        btnSend.addActionListener(e -> sendMessage());
        btnFile.addActionListener(e -> chooseFile());
        txtMessage.addActionListener(e -> sendMessage());
    }

    private int getTtl() {
        switch (cboTtl.getSelectedIndex()) {
            case 0:
                return 10;
            case 1:
                return 30;
            case 2:
                return 60;
            case 3:
                return 300;
            default:
                return 30;
        }
    }

    private void connect() {
        String host = txtHost.getText().trim();

        try {
            int port = Integer.parseInt(txtPort.getText().trim());

            if (host.isEmpty() || port < 1 || port > 65535) {
                throw new IllegalArgumentException(
                        "IP hoặc Port khong hop le."
                );
            }

            lblStatus.setText("Dang ket noi...");
            network.connect(host, port);

        }  catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(
                    this,
                    ex.getMessage(),
                    "Thong tin ket noi khong hop le",
                    JOptionPane.WARNING_MESSAGE
            );
        }
    }

    private void disconnect() {
        network.disconnect();
        lblStatus.setText("Da ngat ket noi");
        appendChat("[He thong] Da ngat ket noi.");
    }

    private void sendMessage() {
        String message = txtMessage.getText().trim();

        if (message.isEmpty()) {
            return;
        }

        if (!network.isConnected()) {
            showNotConnected();
            return;
        }

        int ttl = getTtl();

        network.sendMessage(message, ttl);

        addExpiringItem("Tin nhan cua bạn: " + message, ttl);
        txtMessage.setText("");
    }

    private void chooseFile() {
        if (!network.isConnected()) {
            showNotConnected();
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chon tep can gui");

        if (chooser.showOpenDialog(this)
                != JFileChooser.APPROVE_OPTION) {
            return;
        }

        Path path = chooser.getSelectedFile().toPath();
        int ttl = getTtl();

        network.sendFile(path, ttl);
        addExpiringItem(
                "Tep da gui: " + path.getFileName(),
                ttl
        );
    }

    private void addExpiringItem(String description, int ttl) {
        expiringItems.add(new ExpiringItem(description, ttl));
        appendChat("[Dem nguoc] " + description
                + " — tu huy sau " + ttl + " giay.");
    }

    private void updateCountdowns() {
        long now = Instant.now().getEpochSecond();

        for (int i = expiringItems.size() - 1; i >= 0; i--) {
            ExpiringItem item = expiringItems.get(i);
            long remaining = item.expiresAt - now;

            if (remaining <= 0) {
                appendChat("[Het thoi gian] " + item.description
                        + " — dang cho xac nhan tu Server.");
                expiringItems.remove(i);
            }
        }
    }

    // Các callback từ ClientNetwork.

    @Override
    public void onConnected() {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Da ket noi");
            appendChat("[He thong] Da ket noi Server.");
        });
    }

    @Override
    public void onDisconnected(String reason) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText("Da ngat ket noi");
            appendChat("[He thong] " + reason);
        });
    }

    @Override
    public void onMessage(String sender, String message, int ttl) {
        SwingUtilities.invokeLater(() -> {
            appendChat("[" + sender + "] " + message);

            if (ttl > 0) {
                addExpiringItem(
                        "Tin nhan tu " + sender + ": " + message,
                        ttl
                );
            }
        });
    }

    @Override
    public void onExpired(String id, String name) {
        SwingUtilities.invokeLater(() -> {
            appendChat("[Server] Du lieu da het han: "
                    + name + " (ID: " + id + ")");
        });
    }

    @Override
    public void onInfo(String message) {
        SwingUtilities.invokeLater(() -> appendChat(message));
    }

    @Override
    public void onError(String message) {
        SwingUtilities.invokeLater(() -> {
            appendChat("[Loi] " + message);
            lblStatus.setText("Co loi xay ra");
        });
    }

    private void appendChat(String message) {
        txtChat.append(message + "\n");
        txtChat.setCaretPosition(
                txtChat.getDocument().getLength()
        );
    }

    private void showNotConnected() {
        JOptionPane.showMessageDialog(
                this,
                "Vui long ket noi Server truoc.",
                "Chua ket noi",
                JOptionPane.WARNING_MESSAGE
        );
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ClientApp().setVisible(true);
        });
    }
}
