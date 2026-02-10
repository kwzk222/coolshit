package overlay;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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
                            newBoxes.add(new BoxData(x, y, w, h, label));
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

                g2d.setStroke(new BasicStroke(2.0f));
                g2d.setColor(Color.BLACK);
                g2d.drawRect(bx - 1, by - 1, bw + 2, bh + 2);
                g2d.setColor(Color.WHITE);
                g2d.drawRect(bx, by, bw, bh);

                if (!box.label.isEmpty()) {
                    g2d.setFont(new Font("Consolas", Font.BOLD, 12));
                    FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(box.label);
                    g2d.setColor(new Color(0, 0, 0, 150));
                    g2d.fillRect(bx + (bw - labelWidth) / 2 - 2, by - 15, labelWidth + 4, 13);
                    g2d.setColor(Color.WHITE);
                    g2d.drawString(box.label, bx + (bw - labelWidth) / 2, by - 4);
                }
            }
        }
    }

    private static class BoxData {
        float xf, yf, wf, hf;
        String label;
        BoxData(float xf, float yf, float wf, float hf, String label) {
            this.xf = xf; this.yf = yf; this.wf = wf; this.hf = hf; this.label = label;
        }
    }
}
