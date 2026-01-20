package net.rev.tutorialmod.modules;

import net.rev.tutorialmod.TutorialMod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

public class OverlayManager {

    private static final String OVERLAY_JAR_NAME = "overlay.jar";
    private Process overlayProcess;
    private PrintWriter socketWriter;
    private static final int PORT = 25566;

    public void start() {
        if (isRunning()) {
            return;
        }

        try {
            // 1. Extract the JAR from resources
            File extractedJar = extractJar();
            if (extractedJar == null) {
                System.err.println("[TutorialMod] Could not extract overlay.jar. Make sure it is included in the mod's resources.");
                return;
            }

            // 2. Start the external process
            ProcessBuilder pb = new ProcessBuilder("java", "-jar", extractedJar.getAbsolutePath());
            overlayProcess = pb.start();

            // 3. Connect the socket (in a new thread to not block the game)
            new Thread(() -> {
                try {
                    // Give the process a moment to start up. A retry loop is more robust, but this is simpler.
                    Thread.sleep(1000);

                    Socket socket = new Socket("127.0.0.1", PORT);
                    socketWriter = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
                    sendConfig();
                    System.out.println("[TutorialMod] Connected to overlay app.");
                } catch (Exception e) {
                    System.err.println("[TutorialMod] Could not connect to overlay app: " + e.getMessage());
                    // If connection fails, stop the process
                    stop();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            stop(); // Ensure we clean up if starting fails
        }
    }

    public void stop() {
        if (socketWriter != null) {
            try {
                socketWriter.close();
            } catch (Exception e) {
                // Ignore
            }
            socketWriter = null;
        }
        if (overlayProcess != null) {
            overlayProcess.destroy();
            overlayProcess = null;
        }
    }

    public void update(String coords) {
        if (socketWriter != null) {
            socketWriter.println(coords);
        }
    }

    public void sendConfig() {
        if (socketWriter != null) {
            socketWriter.println("CONFIG FONT_SIZE " + TutorialMod.CONFIG.overlayFontSize);
            socketWriter.println("CONFIG OPACITY " + TutorialMod.CONFIG.overlayBackgroundOpacity);
        }
    }

    public boolean isRunning() {
        return overlayProcess != null && overlayProcess.isAlive();
    }

    private File extractJar() {
        try {
            // Note: The resource path must match how it's stored in the JAR. Usually, it's at the root.
            InputStream jarStream = TutorialMod.class.getClassLoader().getResourceAsStream(OVERLAY_JAR_NAME);
            if (jarStream == null) {
                return null;
            }

            // Extract to the system's temp directory to avoid cluttering the game directory
            File tempFile = new File(System.getProperty("java.io.tmpdir"), OVERLAY_JAR_NAME);
            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = jarStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            // This ensures the temporary file is deleted when the game closes
            tempFile.deleteOnExit();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
