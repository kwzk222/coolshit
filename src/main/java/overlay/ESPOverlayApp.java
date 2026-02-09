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
    private static final int PORT = 25567; // Different port from Coords Overlay

    public static void main(String[] args) {
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

            frame.setSize(800, 600); // Default size
            frame.setLocationRelativeTo(null);
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
                    frame.setBounds(x, y, w, h);
                    if (!frame.isVisible()) {
                        frame.setVisible(true);
                        setClickThrough(true);
                    }
                } catch (Exception ignored) {}
            }
        } else if (content.startsWith("BOXES ")) {
            panel.updateBoxes(content.substring(6));
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

        public ESPPanel() {
            setOpaque(false);
        }

        public void updateBoxes(String data) {
            List<BoxData> newBoxes = new ArrayList<>();
            if (!data.isEmpty()) {
                String[] items = data.split(";");
                for (String item : items) {
                    String[] parts = item.split(",");
                    if (parts.length >= 4) {
                        try {
                            int x = Integer.parseInt(parts[0]);
                            int y = Integer.parseInt(parts[1]);
                            int w = Integer.parseInt(parts[2]);
                            int h = Integer.parseInt(parts[3]);
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

            for (BoxData box : boxes) {
                g2d.setColor(Color.WHITE);
                g2d.drawRect(box.x, box.y, box.w, box.h);
                if (!box.label.isEmpty()) {
                    g2d.setFont(new Font("Consolas", Font.BOLD, 12));
                    FontMetrics fm = g2d.getFontMetrics();
                    int labelWidth = fm.stringWidth(box.label);
                    g2d.drawString(box.label, box.x + (box.w - labelWidth) / 2, box.y - 2);
                }
            }
        }
    }

    private static class BoxData {
        int x, y, w, h;
        String label;
        BoxData(int x, int y, int w, int h, String label) {
            this.x = x; this.y = y; this.w = w; this.h = h; this.label = label;
        }
    }
}
