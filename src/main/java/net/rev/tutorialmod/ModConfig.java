package net.rev.tutorialmod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "tutorialmod.json");

    public int axeSwapDelay = 5;
    public int maceSwapDelay = 1;
    public int comboSwapDelay = 1;
    public int postComboAxeSwapDelay = 2;

    public boolean totemSwapEnabled = true;
    public boolean axeSwapEnabled = true;
    public boolean maceSwapEnabled = true;
    public boolean tntMinecartPlacementEnabled = true;
    public boolean lavaCrossbowSequenceEnabled = true;
    public boolean bowSequenceEnabled = true;
    public int lavaPickupDelay = 5;
    public int minFallDistance = 3;
    public boolean masterEnabled = true;
    public int bowCooldown = 100;

    public List<String> friends = new ArrayList<>();
    public transient TeamManager teamManager;

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
        config.teamManager = new TeamManager(config.friends);
        return config;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            TutorialMod.LOGGER.error("Error saving config", e);
        }
    }
}
