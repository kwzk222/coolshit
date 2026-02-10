package net.rev.tutorialmod.overlay;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.io.File;
import javax.imageio.ImageIO;

public class ESPOverlayApp {
    private static JFrame frame;
    private static ESPPanel panel;
    private static boolean debug = false;
    private static final Map<String, Image> textureCache = new HashMap<>();

    public static void main(String[] args) {
        // We use system scaling so it matches Minecraft's logical coordinate system
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("TutorialMod-ESP");
            frame.setUndecorated(true);
            frame.setBackground(new Color(0, 0, 0, 0));
            frame.setAlwaysOnTop(true);
            frame.setType(Window.Type.UTILITY);
            frame.setFocusableWindowState(false);

            panel = new ESPPanel();
            frame.add(panel);
            frame.setSize(1, 1);
            frame.setVisible(true);

            // Windows-specific transparency/click-through via JNA would go here if needed,
            // but for O-ESP it's mostly visual.
        });

        startServer();
    }

    private static void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(25567)) {
            while (true) {
                try (Socket socket = serverSocket.accept();
                     Scanner scanner = new Scanner(socket.getInputStream())) {
                    while (scanner.hasNextLine()) {
                        String line = scanner.nextLine();
                        handleCommand(line);
                    }
                } catch (Exception e) {
                    System.err.println("Connection lost: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleCommand(String line) {
        if (line.startsWith("WINDOW_SYNC")) {
            String[] parts = line.substring(12).split(",");
            if (parts.length == 4) {
                int x = Integer.parseInt(parts[0]);
                int y = Integer.parseInt(parts[1]);
                int w = Integer.parseInt(parts[2]);
                int h = Integer.parseInt(parts[3]);
                SwingUtilities.invokeLater(() -> {
                    frame.setBounds(x, y, w, h);
                    frame.revalidate();
                });
            }
        } else if (line.startsWith("DEBUG")) {
            debug = Boolean.parseBoolean(line.substring(6));
        } else if (line.startsWith("CLEAR")) {
            panel.setBoxes("");
        } else if (line.startsWith("BOXES ")) {
            panel.setBoxes(line.substring(6));
        }
    }

    private static class ESPPanel extends JPanel {
        private String boxesData = "";

        public ESPPanel() {
            setOpaque(false);
        }

        public void setBoxes(String data) {
            this.boxesData = data;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (boxesData.isEmpty()) return;

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            if (debug) {
                g2.setColor(new Color(255, 0, 0, 50));
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.drawString("O-ESP Active (" + getWidth() + "x" + getHeight() + ")", 10, 20);
            }

            String[] boxes = boxesData.split(";");
            for (String box : boxes) {
                if (box.isEmpty()) continue;
                String[] p = box.split("\\|");
                if (p.length < 4) continue;

                try {
                    int x = Integer.parseInt(p[0]);
                    int y = Integer.parseInt(p[1]);
                    int w = Integer.parseInt(p[2]);
                    int h = Integer.parseInt(p[3]);
                    String label = p.length > 4 ? p[4] : "";
                    int colorInt = p.length > 5 ? Integer.parseInt(p[5]) : 0xFFFFFF;
                    String dist = p.length > 6 ? p[6] : "";
                    String texPath = p.length > 7 ? p[7] : "";

                    Color color = new Color(colorInt);

                    // Draw Texture if available
                    if (!texPath.isEmpty()) {
                        Image img = textureCache.get(texPath);
                        if (img == null) {
                            File f = new File(texPath);
                            if (f.exists()) {
                                try {
                                    img = ImageIO.read(f);
                                    textureCache.put(texPath, img);
                                } catch (Exception ignored) {}
                            }
                        }
                        if (img != null) {
                            g2.drawImage(img, x, y, w, h, null);
                        }
                    }

                    // Draw Box
                    g2.setColor(color);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawRect(x, y, w, h);

                    // Fill slightly
                    g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 30));
                    g2.fillRect(x, y, w, h);

                    // Draw Labels
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    FontMetrics fm = g2.getFontMetrics();

                    if (!label.isEmpty()) {
                        g2.setColor(Color.BLACK);
                        g2.drawString(label, x + (w - fm.stringWidth(label)) / 2 + 1, y - 5 + 1);
                        g2.setColor(Color.WHITE);
                        g2.drawString(label, x + (w - fm.stringWidth(label)) / 2, y - 5);
                    }

                    if (!dist.isEmpty()) {
                        g2.setColor(Color.BLACK);
                        g2.drawString(dist, x + (w - fm.stringWidth(dist)) / 2 + 1, y + h + 12 + 1);
                        g2.setColor(Color.YELLOW);
                        g2.drawString(dist, x + (w - fm.stringWidth(dist)) / 2, y + h + 12);
                    }

                } catch (Exception e) {
                    if (debug) e.printStackTrace();
                }
            }
        }
    }
}
