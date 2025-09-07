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

    // --- Attribute Swapping ---
    public int axeSwapDelay = 5;
    public int maceSwapDelay = 1;
    public int axeToOriginalDelay = 1;
    public int maceToOriginalDelay = 1;

    public boolean axeSwapEnabled = true;
    public boolean maceSwapEnabled = true;

    // --- Minecart Tech ---
    public boolean tntMinecartPlacementEnabled = true;
    public boolean lavaCrossbowSequenceEnabled = true;
    public boolean bowSequenceEnabled = true;
    public int minFallDistance = 3;

    // --- General ---
    public boolean masterEnabled = true;
    public int bowCooldown = 100;

    // --- Auto Tool ---
    public boolean autoToolSwitchEnabled = true;
    public boolean toolDurabilitySafetyEnabled = true;
    public boolean autoToolSwitchBackEnabled = true;
    public int autoToolSwitchBackMinDelay = 0;
    public int autoToolSwitchBackMaxDelay = 5;
    public int autoToolSwitchMineMinDelay = 0;
    public int autoToolSwitchMineMaxDelay = 2;

    // --- TriggerBot ---
    public boolean triggerBotEnabled = true;
    public boolean triggerBotIncludePlayers = true;
    public boolean triggerBotExcludeTeammates = true;
    public boolean triggerBotIncludeHostiles = true;
    public boolean triggerBotIncludePassives = false;
    public boolean triggerBotExcludeVillagers = true;
    public boolean triggerBotIncludeCrystals = true;
    public boolean triggerBotActiveInInventory = false;
    public double triggerBotMaxRange = 4.5;
    public int triggerBotAttackDelay = 0;
    public transient boolean triggerBotToggledOn = true;

    // --- Hotkeys ---
    public String masterToggleHotkey = "key.keyboard.m";
    public String teammateHotkey = "key.keyboard.g";
    public String triggerBotToggleHotkey = "key.keyboard.k";


    public List<String> teammates = new ArrayList<>();
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
        config.teamManager = new TeamManager(config.teammates);
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
