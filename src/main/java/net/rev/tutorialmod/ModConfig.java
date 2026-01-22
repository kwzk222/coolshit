package net.rev.tutorialmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "tutorialmod.json");

    // --- Hotkeys ---
    public String openSettingsHotkey = "key.keyboard.right.shift";
    public String lastCategory = "Overlay";
    public String toggleOverlayHotkey = "key.keyboard.h";
    public String sprintModeHotkey = "key.keyboard.n";
    public String sneakModeHotkey = "key.keyboard.b";
    public boolean activeInInventory = false;

    // --- Chat ---
    public boolean disableModChatUpdates = false;
    public String trigger = "cc";
    public boolean caseSensitive = false;
    public boolean replaceInChat = true;
    public boolean replaceInCommands = true;
    public String format = "{bx} {by} {bz} {dim} {facing}";

    public Macro macro1 = new Macro();
    public Macro macro2 = new Macro();
    public Macro macro3 = new Macro();
    public Macro macro4 = new Macro();
    public Macro macro5 = new Macro();

    public static class Macro {
        public String name = "New Macro";
        public String hotkey = "key.keyboard.unknown";
        public String message = "";
    }

    // --- Coords Overlay ---
    public boolean showCoordsOverlay = false;
    public boolean showAccurateCoordinates = false;
    public boolean showEntityCount = false;
    public boolean showChunkCount = false;
    public boolean showDetailedCardinals = false;
    public boolean showLongCoords = false;
    public int longCoordsMaxDistance = 512;
    public boolean showLongCoordsDistance = true;
    public boolean showNetherCoords = false;
    public boolean showSprintModeOverlay = true;
    public boolean showSneakModeOverlay = true;
    public int overlayFontSize = 20;
    public int overlayBackgroundOpacity = 128;
    public String overlayAlignment = "Left";
    public String overlayFontName = "Consolas";
    public boolean overlayLocked = false;

    public static ModConfig load() {
        ModConfig config;
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                TutorialMod.LOGGER.error("Error loading config", e);
                config = new ModConfig();
            }
        } else {
            config = new ModConfig();
        }
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
            // Update overlay config if it's running
            if (TutorialModClient.getInstance() != null && TutorialModClient.getOverlayManager() != null) {
                TutorialModClient.getOverlayManager().sendConfig();
            }
        } catch (IOException e) {
            TutorialMod.LOGGER.error("Error saving config", e);
        }
    }
}
