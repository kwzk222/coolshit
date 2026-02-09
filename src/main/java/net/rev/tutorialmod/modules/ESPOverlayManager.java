package net.rev.tutorialmod.modules;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class ESPOverlayManager {

    private static final String OVERLAY_JAR_NAME = "esp_overlay.jar";
    private Process overlayProcess;
    private PrintWriter socketWriter;
    private static final int PORT = 25567;

    public void start() {
        if (isRunning()) {
            return;
        }

        try {
            File extractedJar = extractJar();
            if (extractedJar == null) {
                System.err.println("[TutorialMod] Could not extract esp_overlay.jar.");
                return;
            }


            ProcessBuilder pb = new ProcessBuilder("java", "-jar", extractedJar.getAbsolutePath());
            overlayProcess = pb.start();

            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Socket socket = new Socket("127.0.0.1", PORT);
                    socketWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                    System.out.println("[TutorialMod] Connected to ESP overlay app.");
                    // Sync window immediately on connect
                    TutorialModClient.getInstance().getESPModule().syncWindowBounds();
                } catch (Exception e) {
                    System.err.println("[TutorialMod] Could not connect to ESP overlay app: " + e.getMessage());
                    stop();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            stop();
        }
    }

    public void stop() {
        if (socketWriter != null) {
            try {
                socketWriter.close();
            } catch (Exception ignored) {}
            socketWriter = null;
        }
        if (overlayProcess != null) {
            overlayProcess.destroy();
            overlayProcess = null;
        }
    }

    public void updateBoxes(String boxesData) {
        if (socketWriter != null) {
            socketWriter.println("BOXES " + boxesData);
        }
    }

    public void sendCommand(String command) {
        if (socketWriter != null) {
            socketWriter.println(command);
        }
    }

    public boolean isRunning() {
        return overlayProcess != null && overlayProcess.isAlive();
    }

    private File extractJar() {
        try {
            InputStream jarStream = TutorialMod.class.getClassLoader().getResourceAsStream(OVERLAY_JAR_NAME);
            if (jarStream == null) {
                return null;
            }
            File tempFile = new File(System.getProperty("java.io.tmpdir"), OVERLAY_JAR_NAME);
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = jarStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            tempFile.deleteOnExit();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
