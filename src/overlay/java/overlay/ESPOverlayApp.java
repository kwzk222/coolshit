package overlay;

import javax.imageio.ImageIO;
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

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinUser;

public class ESPOverlayApp {
    private static JFrame frame;
    private static ESPPanel panel;
    private static final int PORT = 25567;

    public static void main(String[] args) {
        System.setProperty("sun.java2d.uiScale", "1.0");

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
        } else if (content.startsWith("HEALTH_BAR_WIDTH ")) {
            panel.setHealthBarWidth(Float.parseFloat(content.substring(17)));
        } else if (content.startsWith("HEALTH_BAR_INVERTED ")) {
            panel.setHealthBarInverted(Boolean.parseBoolean(content.substring(20)));
        } else if (content.startsWith("DEBUG_TEXT ")) {
            panel.setDebugText(content.substring(11));
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
            WinClickThrough.setClickThrough(frame, enabled);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static class WinClickThrough {
        public static void setClickThrough(JFrame frame, boolean enabled) {
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
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static class ESPPanel extends JPanel {
        private List<BoxData> boxes = new ArrayList<>();
        private boolean debugMode = true;
        private String debugText = "";
        private float healthBarWidth = 0.1f;
        private boolean healthBarInverted = false;
        private final Map<String, BufferedImage> textureCache = new HashMap<>();
        private static final String TEXTURE_DIR = "tutorialmod_textures";

        public ESPPanel() {
            setOpaque(false);
        }

        public void setDebugMode(boolean enabled) {
            this.debugMode = enabled;
            repaint();
        }

        public void setDebugText(String text) {
            this.debugText = text;
            repaint();
        }

        public void setHealthBarWidth(float width) {
            this.healthBarWidth = width;
        }

        public void setHealthBarInverted(boolean inverted) {
            this.healthBarInverted = inverted;
        }

        public void updateBoxes(String data) {
            List<BoxData> newBoxes = new ArrayList<>();
            if (!data.isEmpty()) {
                String[] items = data.split(";");
                for (String item : items) {
                    String[] parts = item.split(",");
                    if (parts.length >= 4) {
                        try {
                            float x = Float.parseFloat(parts[0]);
                            float y = Float.parseFloat(parts[1]);
                            float w = Float.parseFloat(parts[2]);
                            float h = Float.parseFloat(parts[3]);
                            String label = parts.length > 4 ? parts[4] : "";
                            int color = parts.length > 5 ? Integer.parseInt(parts[5]) : 0xFFFFFF;
                            String distLabel = parts.length > 6 ? parts[6] : "";
                            float health = parts.length > 7 ? Float.parseFloat(parts[7]) : -1f;
                            String texture = parts.length > 8 ? parts[8] : "";
                            newBoxes.add(new BoxData(x, y, w, h, label, color, distLabel, health, texture));
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

            int panelW = getWidth();
            int panelH = getHeight();

            if (debugMode) {
                g2d.setColor(new Color(255, 0, 0, 100));
                g2d.setStroke(new BasicStroke(4.0f));
                g2d.drawRect(0, 0, panelW - 1, panelH - 1);

                g2d.setStroke(new BasicStroke(1.0f));
                g2d.drawLine(panelW / 2 - 20, panelH / 2, panelW / 2 + 20, panelH / 2);
                g2d.drawLine(panelW / 2, panelH / 2 - 20, panelW / 2, panelH / 2 + 20);

                g2d.setFont(new Font("Arial", Font.BOLD, 14));
                g2d.drawString("ESP Overlay (" + panelW + "x" + panelH + ")", 10, 20);
                g2d.drawString("CENTER: " + (panelW / 2) + ", " + (panelH / 2), 10, 40);
                g2d.drawString(debugText, 10, 60);
            }

            for (BoxData box : boxes) {
                int bx = (int) (box.xf * panelW);
                int by = (int) (box.yf * panelH);
                int bw = (int) (box.wf * panelW);
                int bh = (int) (box.hf * panelH);

                if (bw <= 0 || bh <= 0) continue;

                Color c = new Color(box.color | 0xFF000000, true);

                if (!box.texture.isEmpty()) {
                    BufferedImage img = getTexture(box.texture);
                    if (img != null) {
                        g2d.drawImage(img, bx, by, bw, bh, null);
                    } else {
                        // Fallback to outline if texture missing
                        g2d.setStroke(new BasicStroke(2.0f));
                        g2d.setColor(Color.BLACK);
                        g2d.drawRect(bx - 1, by - 1, bw + 2, bh + 2);
                        g2d.setColor(c);
                        g2d.drawRect(bx, by, bw, bh);
                    }
                } else {
                    g2d.setStroke(new BasicStroke(2.0f));
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(bx - 1, by - 1, bw + 2, bh + 2);
                    g2d.setColor(c);
                    g2d.drawRect(bx, by, bw, bh);
                }

                // Health Bar
                if (box.health >= 0) {
                    drawHealthBar(g2d, bx, by, bw, bh, box.health);
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

        private BufferedImage getTexture(String name) {
            if (textureCache.containsKey(name)) return textureCache.get(name);
            try {
                File f = new File(TEXTURE_DIR, name + ".png");
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    textureCache.put(name, img);
                    return img;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            textureCache.put(name, null); // Don't try again if failed
            return null;
        }

        private void drawHealthBar(Graphics2D g2d, int bx, int by, int bw, int bh, float health) {
            int barW = (int)(bw * healthBarWidth);
            if (barW < 2) barW = 2;
            int barX = bx + bw + 3;
            int barY = by;
            int barH = bh;

            // Background (Black/Empty)
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(barX, barY, barW, barH);

            // Health Color (Green to Red?) - User said "green should represent their hearts and black should represent empty hearts"
            // So just Green.
            g2d.setColor(Color.GREEN);
            int healthH = (int)(barH * Math.max(0, Math.min(1, health)));

            if (healthBarInverted) {
                // Inverted: Health at bottom, black at top (drains from top)
                g2d.fillRect(barX, barY + (barH - healthH), barW, healthH);
            } else {
                // Default: Health at top, black at bottom (shrinks upwards, drains from bottom)
                g2d.fillRect(barX, barY, barW, healthH);
            }

            // Outline
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawRect(barX, barY, barW, barH);
        }
    }

    private static class BoxData {
        float xf, yf, wf, hf;
        String label;
        int color;
        String distLabel;
        float health;
        String texture;
        BoxData(float xf, float yf, float wf, float hf, String label, int color, String distLabel, float health, String texture) {
            this.xf = xf; this.yf = yf; this.wf = wf; this.hf = hf; this.label = label; this.color = color; this.distLabel = distLabel;
            this.health = health; this.texture = texture;
        }
    }
}
