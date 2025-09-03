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
    public int minFallDistance = 3;
    public boolean masterEnabled = true;
    public int bowCooldown = 100;

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

    public List<String> teammates = new ArrayList<>();
    public transient TeamManager teamManager;

    public MovementParams movementParams = new MovementParams();

    public static class MovementParams {
        // Duration selection
        public float baseMs = 120f;           // base time per move
        public float kPerUnit = 6f;           // ms per deg (camera) or per px (GUI)
        public float minMs = 60f, maxMs = 600f;

        // Reaction delay before movement begins
        public float reactMinMs = 100f, reactMaxMs = 250f;

        // Corrections (closed-loop)
        public float correctionThreshold = 0.3f; // deg or px
        public int maxCorrections = 3;
        public float correctionDurScale = 0.25f; // fraction of main duration

        // Noise (signal-dependent)
        public float alphaNoise = 0.003f;     // scales with speed
        public float noiseJitterFactor = 0.3f;// randomness on alpha

        // Tremor (high frequency micro jitter)
        public float tremorAmp = 0.03f;       // deg/px
        public float tremorF1 = 9f, tremorF2 = 22f;

        // Low-frequency drift (optional)
        public float driftAmp = 0.02f;        // deg/px
        public float driftHz = 1.2f;

        // Geometry / curvature
        public float viaPointChance = 0.35f;
        public float viaOffsetMin = 0.05f, viaOffsetMax = 0.25f; // fraction of distance

        // Sampling / application
        public int sampleHz = 120;            // 60–240 good
        public float blend = 0.9f;            // micro-lag smoothing 0..1

        // Safety
        public float pitchMin = -90f, pitchMax = 90f;
    }

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
