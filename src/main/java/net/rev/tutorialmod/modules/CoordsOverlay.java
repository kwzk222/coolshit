package net.rev.tutorialmod.modules;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CoordsOverlay {

    private static CoordsOverlay INSTANCE;

    private JFrame overlayFrame;
    private JLabel coordsLabel;
    private String lastText = "Waiting for player...";

    private CoordsOverlay() {
        // Private constructor for singleton
    }

    public static CoordsOverlay getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CoordsOverlay();
        }
        return INSTANCE;
    }

    // This method must be called on the EDT
    private void buildOrRebuildFrame() {
        try {
            if (overlayFrame != null) {
                overlayFrame.dispose();
            }

            overlayFrame = new JFrame("Coords Overlay");

            // Configure based on ModConfig
            overlayFrame.setUndecorated(TutorialMod.CONFIG.overlayUndecorated);
            overlayFrame.setAlwaysOnTop(true);
            overlayFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            overlayFrame.setFocusableWindowState(false);
            overlayFrame.setType(Window.Type.UTILITY);

            if (TutorialMod.CONFIG.overlayTransparent) {
                overlayFrame.setBackground(new Color(0, 0, 0, 0));
            }

            coordsLabel = new JLabel(lastText, SwingConstants.CENTER);
            coordsLabel.setFont(new Font("Consolas", Font.BOLD, 18));
            coordsLabel.setForeground(Color.WHITE);
            overlayFrame.add(coordsLabel);

            overlayFrame.setSize(300, 50);
            overlayFrame.setLocationRelativeTo(null);

            // Set visibility based on the current config
            overlayFrame.setVisible(TutorialMod.CONFIG.showCoordsOverlay);
        } catch (Throwable t) {
            TutorialModClient.showChatMessage("Failed to create overlay: " + t.getMessage());
            TutorialModClient.showChatMessage("A full error log is being saved to the config folder.");

            try {
                File errorLogFile = new File(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().toFile(), "tutorialmod_overlay_error.log");
                try (PrintWriter fileWriter = new PrintWriter(new FileWriter(errorLogFile))) {
                    fileWriter.println("Error occurred on: " + new java.util.Date());
                    t.printStackTrace(fileWriter);
                }
                TutorialModClient.showChatMessage("Error log saved to: " + errorLogFile.getAbsolutePath());
            } catch (Exception e) {
                TutorialModClient.showChatMessage("Critical: Could not write error log file: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void create() {
        // Initial creation
        SwingUtilities.invokeLater(this::buildOrRebuildFrame);
    }

    public void restyle() {
        // Re-create with new styles
        SwingUtilities.invokeLater(this::buildOrRebuildFrame);
    }

    public void update(String text) {
        lastText = text; // Keep track of the last text
        if (coordsLabel != null) {
            SwingUtilities.invokeLater(() -> coordsLabel.setText(text));
        }
    }

    public void show() {
        if (overlayFrame != null) {
            SwingUtilities.invokeLater(() -> overlayFrame.setVisible(true));
        } else {
            // If frame doesn't exist, create it. It will become visible based on config.
            create();
        }
    }

    public void hide() {
        if (overlayFrame != null) {
            SwingUtilities.invokeLater(() -> overlayFrame.setVisible(false));
        }
    }

    public void toggle(boolean show) {
        // The config is already set by the menu, so we just need to apply the visibility.
        if (show) {
            show();
        } else {
            hide();
        }
    }
}
