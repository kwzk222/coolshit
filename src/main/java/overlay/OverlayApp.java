package overlay;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Properties;

public class OverlayApp {
    private static JLabel infoLabel;
    private static Point initialClick;
    private static JFrame frame;
    private static final File CONFIG_FILE = new File("overlay.properties");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame();
            frame.setUndecorated(true); // no border
            frame.setAlwaysOnTop(true);
            frame.setFocusableWindowState(false);
            frame.setBackground(new Color(0, 0, 0, 0)); // fully transparent background
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            // Transparent panel with semi-transparent background
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    Object opacityObj = frame.getRootPane().getClientProperty("opacity");
                    int opacity = (opacityObj instanceof Integer) ? (Integer) opacityObj : 128;
                    g2d.setColor(new Color(0, 0, 0, opacity)); // black with configurable opacity
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                }
            };
            panel.setOpaque(false);
            panel.setLayout(new BorderLayout());

            infoLabel = new JLabel("", SwingConstants.LEFT);
            infoLabel.setFont(new Font("Consolas", Font.BOLD, 20));
            infoLabel.setForeground(Color.WHITE);
            infoLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // Add padding

            panel.add(infoLabel, BorderLayout.CENTER);
            frame.setContentPane(panel);

            // Load saved window position + size
            loadWindowBounds();

            frame.setVisible(false);

            // Make draggable
            panel.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    initialClick = e.getPoint();
                }
            });
            panel.addMouseMotionListener(new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    int thisX = frame.getLocation().x;
                    int thisY = frame.getLocation().y;
                    int xMoved = e.getX() - initialClick.x;
                    int yMoved = e.getY() - initialClick.y;
                    frame.setLocation(thisX + xMoved, thisY + yMoved);
                }
            });

            // Allow resizing by dragging edges
            frame.getRootPane().setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            frame.addComponentListener(new java.awt.event.ComponentAdapter() {
                @Override
                public void componentResized(java.awt.event.ComponentEvent e) {
                    saveWindowBounds();
                }

                @Override
                public void componentMoved(java.awt.event.ComponentEvent e) {
                    saveWindowBounds();
                }
            });

            // Add resizing
            ComponentResizer cr = new ComponentResizer();
            cr.registerComponent(panel);
        });

        // Socket listener thread
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(25566)) {
                System.out.println("[OverlayApp] Listening on port 25566...");
                Socket client = server.accept();
                System.out.println("[OverlayApp] Connected!");

                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    final String content = line;
                    SwingUtilities.invokeLater(() -> {
                        if (content.startsWith("CONFIG ")) {
                            String[] parts = content.split(" ");
                            if (parts.length >= 3) {
                                String key = parts[1];
                                String value = parts[2];
                                switch (key) {
                                    case "FONT_SIZE":
                                        try {
                                            int size = Integer.parseInt(value);
                                            infoLabel.setFont(new Font("Consolas", Font.BOLD, size));
                                            frame.pack();
                                        } catch (Exception ignored) {}
                                        break;
                                    case "OPACITY":
                                        try {
                                            int opacity = Integer.parseInt(value);
                                            // The panel's paintComponent uses this indirectly if we store it
                                            frame.getRootPane().putClientProperty("opacity", opacity);
                                            frame.repaint();
                                        } catch (Exception ignored) {}
                                        break;
                                }
                            }
                        } else if (content.trim().isEmpty()) {
                            frame.setVisible(false);
                        } else {
                            String htmlContent = content.replace("\\n", "<br>");
                            infoLabel.setText("<html><div style='text-align: left;'>" + htmlContent + "</div></html>");
                            if (!frame.isVisible()) {
                                frame.setVisible(true);
                            }
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Save window bounds on shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(OverlayApp::saveWindowBounds));
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

            frame.setBounds(x, y, w, h);
        } catch (Exception e) {
            e.printStackTrace();
            frame.setSize(260, 80);
            frame.setLocationRelativeTo(null);
        }
    }

    private static void saveWindowBounds() {
        if (frame == null) return;

        Properties props = new Properties();
        Rectangle bounds = frame.getBounds();
        props.setProperty("x", String.valueOf(bounds.x));
        props.setProperty("y", String.valueOf(bounds.y));
        props.setProperty("width", String.valueOf(bounds.width));
        props.setProperty("height", String.valueOf(bounds.height));

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            props.store(out, "OverlayApp Window Bounds");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
