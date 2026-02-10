package overlay;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

public class ESPOverlayApp {
    private static JFrame frame;
    private static ESPPanel panel;
    private static final int PORT = 25567;

    public static void main(String[] args) {
        // Standard OS scaling (Logical coordinates) to match the confirmred working 'red box'

        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("TutorialMod ESP Overlay");
            frame.setUndecorated(true);
            frame.setAlwaysOnTop(true);
            frame.setBackground(new Color(0, 0, 0, 0));
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setFocusableWindowState(false);
            frame.setType(Window.Type.UTILITY);

            panel = new ESPPanel();
            frame.setContentPane(panel);

            frame.setSize(1, 1);
            frame.setLocation(0, 0);
            frame.setVisible(false);
        });

        startServer();
    }

    private static void startServer() {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(PORT)) {
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
        if (content.startsWith("WINDOW_SYNC ")) {
            String[] parts = content.substring(12).split(",");
            if (parts.length == 4) {
                try {
                    int x = Integer.parseInt(parts[0]);
                    int y = Integer.parseInt(parts[1]);
                    int w = Integer.parseInt(parts[2]);
                    int h = Integer.parseInt(parts[3]);
                    if (w > 10 && h > 10) {
                        frame.setBounds(x, y, w, h);
                        if (!frame.isVisible()) {
                            frame.setVisible(true);
                            setClickThrough(true);
                        }
                    }
                } catch (Exception ignored) {}
            }
        } else if (content.startsWith("BOXES ")) {
            panel.updateBoxes(content.substring(6));
        } else if (content.startsWith("DEBUG ")) {
            panel.setDebugMode(Boolean.parseBoolean(content.substring(6)));
        } else if (content.equals("CLEAR")) {
            panel.clear();
        } else if (content.equals("HIDE")) {
            frame.setVisible(false);
        } else if (content.equals("SHOW")) {
            frame.setVisible(true);
            setClickThrough(true);
        }
    }

    private static void setClickThrough(boolean enabled) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) return;
        try {
            HWND hwnd = new HWND();
            hwnd.setPointer(Native.getComponentPointer(frame));
            int exStyle = User32.INSTANCE.GetWindowLong(hwnd, WinUser.GWL_EXSTYLE);
            if (enabled) {
                exStyle |= WinUser.WS_EX_TRANSPARENT | WinUser.WS_EX_LAYERED;
            } else {
                exStyle &= ~WinUser.WS_EX_TRANSPARENT;
            }
            User32.INSTANCE.SetWindowLong(hwnd, WinUser.GWL_EXSTYLE, exStyle);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static class ESPPanel extends JPanel {
        private List<BoxData> boxes = new ArrayList<>();
        private Map<String, BufferedImage> imageCache = new HashMap<>();
        private boolean debugMode = true;

        public ESPPanel() {
            setOpaque(false);
        }

        public void setDebugMode(boolean enabled) {
            this.debugMode = enabled;
            repaint();
        }

        public void updateBoxes(String data) {
            List<BoxData> newBoxes = new ArrayList<>();
            if (!data.isEmpty()) {
                String[] items = data.split(";");
                for (String item : items) {
                    String[] parts = item.split("\\|", -1);
                    if (parts.length >= 4) {
                        try {
                            int x = Integer.parseInt(parts[0]);
                            int y = Integer.parseInt(parts[1]);
                            int w = Integer.parseInt(parts[2]);
                            int h = Integer.parseInt(parts[3]);
                            String label = parts.length > 4 ? parts[4] : "";
                            int color = (parts.length > 5 && !parts[5].isEmpty()) ? Integer.parseInt(parts[5]) : 0xFFFFFF;
                            String distLabel = parts.length > 6 ? parts[6] : "";
                            String texturePath = parts.length > 7 ? parts[7] : "";
                            newBoxes.add(new BoxData(x, y, w, h, label, color, distLabel, texturePath));

                            if (!texturePath.isEmpty() && !imageCache.containsKey(texturePath)) {
                                try {
                                    File f = new File(texturePath);
                                    if (f.exists()) {
                                        imageCache.put(texturePath, ImageIO.read(f));
                                    }
                                } catch (Exception ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }
            this.boxes = newBoxes;
            repaint();
        }

        public void clear() {
            this.boxes.clear();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (debugMode) {
                g2d.setColor(new Color(255, 0, 0, 100));
                g2d.setStroke(new BasicStroke(4.0f));
                g2d.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }

            for (BoxData box : boxes) {
                int bx = box.x;
                int by = box.y;
                int bw = box.w;
                int bh = box.h;
                if (bw <= 0 || bh <= 0) continue;

                if (!box.texturePath.isEmpty() && imageCache.containsKey(box.texturePath)) {
                    BufferedImage img = imageCache.get(box.texturePath);
                    g2d.drawImage(img, bx, by, bw, bh, null);
                    g2d.setColor(new Color(box.color | 0xAA000000, true));
                    g2d.setStroke(new BasicStroke(1.0f));
                    g2d.drawRect(bx, by, bw, bh);
                } else {
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(bx - 1, by - 1, bw + 2, bh + 2);
                    g2d.setColor(new Color(box.color | 0xFF000000, true));
                    g2d.drawRect(bx, by, bw, bh);
                }

                int labelY = by - 4;
                if (!box.label.isEmpty() || !box.distLabel.isEmpty()) {
                    g2d.setFont(new Font("Consolas", Font.BOLD, 12));
                    FontMetrics fm = g2d.getFontMetrics();
                    if (!box.label.isEmpty()) {
                        int labelWidth = fm.stringWidth(box.label);
                        g2d.setColor(new Color(0, 0, 0, 150));
                        g2d.fillRect(bx + (bw - labelWidth) / 2 - 2, labelY - 11, labelWidth + 4, 13);
                        g2d.setColor(Color.WHITE);
                        g2d.drawString(box.label, bx + (bw - labelWidth) / 2, labelY);
                        labelY -= 15;
                    }
                    if (!box.distLabel.isEmpty()) {
                        int distWidth = fm.stringWidth(box.distLabel);
                        g2d.setColor(new Color(0, 0, 0, 150));
                        g2d.fillRect(bx + (bw - distWidth) / 2 - 2, labelY - 11, distWidth + 4, 13);
                        g2d.setColor(Color.WHITE);
                        g2d.drawString(box.distLabel, bx + (bw - distWidth) / 2, labelY);
                    }
                }
            }
        }
    }

    private static class BoxData {
        int x, y, w, h;
        String label, distLabel, texturePath;
        int color;
        BoxData(int x, int y, int w, int h, String label, int color, String distLabel, String texturePath) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label; this.color = color; this.distLabel = distLabel; this.texturePath = texturePath;
        }
    }
}
