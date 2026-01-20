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

    // --- Attribute Swapping ---
    public int fakePredictionChance = 0;
    public int axeSwapFailChance = 0;
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

    // --- Movement ---
    public boolean parkourEnabled = false;
    public double parkourPredict = 0.12;
    public double parkourMaxDropHeight = 0.6;
    public double bridgeAssistPredict = 0.16;
    public double bridgeAssistStartSneakHeight = 0.7;
    public double bridgeAssistStopSneakHeight = 0.5;
    public int bridgeAssistMinHoldTicks = 3;

    // --- Clutch ---
    public boolean clutchEnabled = false;
    public boolean waterClutchEnabled = true;
    public boolean windClutchEnabled = true;
    public double clutchMinFallDistance = 3.0;
    public double clutchActivationPitch = 60.0;
    public int clutchRecoveryDelay = 20;
    public int clutchSwitchDelay = 0;
    public int clutchRestoreDelay = 5;
    public boolean clutchRestoreOriginalSlot = true;
    public boolean clutchAutoSwitch = true;
    public double windClutchMinFallDistance = 8.0;
    public int windClutchFireTicks = 6;
    public int windClutchHighFallFireTicks = 3;
    public int windClutchMaxRetries = 2;
    public double windClutchSuccessVyDelta = 0.5;

    // --- Click Spam ---
    public boolean clickSpamEnabled = false;
    public int clickSpamCps = 12;
    public String clickSpamModifierKey = "key.keyboard.apostrophe";

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

    // --- Auto Totem ---
    public boolean autoTotemEnabled = true;
    public boolean autoTotemSurvivalOnly = true;
    public boolean autoTotemRefillOnPop = true;
    public List<Integer> autoTotemHotbarSlots = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));

    // --- TriggerBot ---
    public int triggerBotMinDelay = 0;
    public int triggerBotMaxDelay = 0;
    public boolean attackOnCrit = false;
    public boolean triggerBotEnabled = true;
    public boolean triggerBotIncludePlayers = true;
    public boolean triggerBotExcludeTeammates = true;
    public boolean triggerBotIncludeHostiles = true;
    public boolean triggerBotIncludePassives = false;
    public boolean triggerBotExcludeVillagers = true;
    public boolean triggerBotIncludeCrystals = true;
    public boolean triggerBotActiveInInventory = false;
    public transient boolean triggerBotToggledOn = true;

    // --- Hotkeys ---
    public String openSettingsHotkey = "key.keyboard.right.shift";
    public String masterToggleHotkey = "key.keyboard.m";
    public String teammateHotkey = "key.keyboard.g";
    public String triggerBotToggleHotkey = "key.keyboard.k";
    public String triggerBotHotkey = "key.keyboard.unknown";
    public String toggleOverlayHotkey = "key.keyboard.h";
    public String parkourHotkey = "key.keyboard.p";
    public String clutchHotkey = "key.keyboard.j";
    public String bridgeAssistHotkey = "key.keyboard.left.alt";
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
    public boolean showSprintModeOverlay = true;
    public boolean showSneakModeOverlay = true;

    // --- Enemy Info ---
    public boolean showEnemyInfo = true;
    public boolean showHpDecimals = false;
    public boolean showLowestArmorPiece = false;
    public boolean doubleEnemyInfoRange = false;


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
