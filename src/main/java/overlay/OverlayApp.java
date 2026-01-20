package overlay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class OverlayApp {
    private static JFrame frame;
    private static JLabel infoLabel;
    private static Point initialClick;
    private static final File CONFIG_FILE = new File("overlay_config.properties");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("TutorialMod Overlay");
            frame.setUndecorated(true);
            frame.setAlwaysOnTop(true);
            frame.setBackground(new Color(0, 0, 0, 0));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setFocusableWindowState(false);

            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    Object opacityObj = frame.getRootPane().getClientProperty("opacity");
                    int opacity = (opacityObj instanceof Integer) ? (Integer) opacityObj : 128;
                    g2d.setColor(new Color(0, 0, 0, opacity));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                }
            };
            panel.setOpaque(false);
            panel.setLayout(new BorderLayout());

            infoLabel = new JLabel("", SwingConstants.LEFT);
            infoLabel.setFont(new Font("Consolas", Font.BOLD, 20));
            infoLabel.setForeground(Color.WHITE);
            infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            panel.add(infoLabel, BorderLayout.CENTER);
            frame.setContentPane(panel);

            loadWindowBounds();
            frame.setVisible(false);

            // Dragging logic
            MouseAdapter dragListener = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (isLocked()) return;
                    initialClick = e.getPoint();
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (isLocked() || initialClick == null) return;
                    int thisX = frame.getLocation().x;
                    int thisY = frame.getLocation().y;
                    int xMoved = e.getX() - initialClick.x;
                    int yMoved = e.getY() - initialClick.y;
                    frame.setLocation(thisX + xMoved, thisY + yMoved);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!isLocked()) saveWindowBounds();
                }
            };
            panel.addMouseListener(dragListener);
            panel.addMouseMotionListener(dragListener);
            infoLabel.addMouseListener(dragListener);
            infoLabel.addMouseMotionListener(dragListener);

            // Resizing logic
            ComponentResizer cr = new ComponentResizer();
            cr.registerComponent(panel);
        });

        startServer();
    }

    private static boolean isLocked() {
        Object lockedObj = frame.getRootPane().getClientProperty("locked");
        return (lockedObj instanceof Boolean && (Boolean) lockedObj);
    }

    private static void startServer() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(25566)) {
                while (true) {
                    try (Socket client = server.accept();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            final String content = line;
                            SwingUtilities.invokeLater(() -> handleMessage(content));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void handleMessage(String content) {
        if (content.startsWith("CONFIG ")) {
            String[] parts = content.split(" ", 3);
            if (parts.length >= 3) {
                String key = parts[1];
                String value = parts[2];
                switch (key) {
                    case "FONT_SIZE":
                        try {
                            int size = Integer.parseInt(value);
                            infoLabel.setFont(new Font(infoLabel.getFont().getName(), Font.BOLD, size));
                        } catch (Exception ignored) {}
                        break;
                    case "FONT_NAME":
                        try {
                            String fontName = value.replace("_", " ");
                            infoLabel.setFont(new Font(fontName, Font.BOLD, infoLabel.getFont().getSize()));
                        } catch (Exception ignored) {}
                        break;
                    case "ALIGNMENT":
                        String align = value.toLowerCase();
                        int swingAlign = SwingConstants.LEFT;
                        String cssAlign = "left";
                        if (align.equals("center") || align.equals("middle")) {
                            swingAlign = SwingConstants.CENTER;
                            cssAlign = "center";
                        } else if (align.equals("right")) {
                            swingAlign = SwingConstants.RIGHT;
                            cssAlign = "right";
                        }
                        infoLabel.setHorizontalAlignment(swingAlign);
                        frame.getRootPane().putClientProperty("textAlign", cssAlign);
                        updateText(infoLabel.getText()); // Refresh alignment in HTML
                        break;
                    case "LOCKED":
                        frame.getRootPane().putClientProperty("locked", Boolean.parseBoolean(value));
                        break;
                    case "OPACITY":
                        try {
                            frame.getRootPane().putClientProperty("opacity", Integer.parseInt(value));
                            frame.repaint();
                        } catch (Exception ignored) {}
                        break;
                }
            }
        } else if (content.trim().isEmpty()) {
            frame.setVisible(false);
            infoLabel.setText("");
        } else {
            updateText(content);
            if (!frame.isVisible()) frame.setVisible(true);
        }
    }

    private static void updateText(String text) {
        if (text == null || text.isEmpty()) return;
        String rawContent = text.startsWith("<html>") ?
            text.replaceAll("<html><div style='text-align: [a-z]+;'>", "").replaceAll("</div></html>", "") :
            text.replace("\\n", "<br>");

        Object textAlignObj = frame.getRootPane().getClientProperty("textAlign");
        String textAlign = (textAlignObj instanceof String) ? (String) textAlignObj : "left";
        infoLabel.setText("<html><div style='text-align: " + textAlign + ";'>" + rawContent + "</div></html>");
    }

    private static void loadWindowBounds() {
        if (!CONFIG_FILE.exists()) {
            frame.setSize(260, 80);
            frame.setLocationRelativeTo(null);
            return;
        }
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            Properties props = new Properties();
            props.load(in);
            int x = Integer.parseInt(props.getProperty("x", "100"));
            int y = Integer.parseInt(props.getProperty("y", "100"));
            int w = Integer.parseInt(props.getProperty("width", "260"));
            int h = Integer.parseInt(props.getProperty("height", "80"));
            frame.setBounds(x, y, Math.max(w, 50), Math.max(h, 20));
        } catch (Exception e) {
            frame.setSize(260, 80);
            frame.setLocationRelativeTo(null);
        }
    }

    private static void saveWindowBounds() {
        if (frame == null || isLocked()) return;
        Properties props = new Properties();
        Rectangle bounds = frame.getBounds();
        props.setProperty("x", String.valueOf(bounds.x));
        props.setProperty("y", String.valueOf(bounds.y));
        props.setProperty("width", String.valueOf(bounds.width));
        props.setProperty("height", String.valueOf(bounds.height));
        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "Overlay Window Bounds");
        } catch (IOException ignored) {}
    }
}
