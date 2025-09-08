package net.rev.tutorialmod.modules;

import net.rev.tutorialmod.TutorialMod;

import javax.swing.*;
import java.awt.*;

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

    private void buildFrame() {
        SwingUtilities.invokeLater(() -> {
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

            overlayFrame.setVisible(TutorialMod.CONFIG.showCoordsOverlay);
        });
    }

    public void create() {
        if (overlayFrame == null) {
            buildFrame();
        }
    }

    public void restyle() {
        if (overlayFrame != null) {
            SwingUtilities.invokeLater(() -> {
                overlayFrame.dispose();
                overlayFrame = null; // Important to nullify
                buildFrame();
            });
        }
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
        }
    }

    public void hide() {
        if (overlayFrame != null) {
            SwingUtilities.invokeLater(() -> overlayFrame.setVisible(false));
        }
    }

    public void toggle(boolean show) {
        if (overlayFrame == null) {
            create();
        }

        if (show) {
            show();
        } else {
            hide();
        }
    }
}
