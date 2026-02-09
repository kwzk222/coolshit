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
    // Axe AutoStun (Sword/others -> Axe)
    public boolean axeSwapEnabled = true;
    public double axeSwapRange = 3.1;
    public int axeSwapDelay = 1;
    public int axeSwapFailChance = 0;
    public int axeSwapFakePredictionChance = 0;

    // Mace AutoStun (Mace -> Axe)
    public boolean maceAutoStunEnabled = true;
    public double maceAutoStunRange = 3.1;
    public int maceAutoStunDelay = 1;
    public int maceAutoStunFailChance = 0;
    public int maceAutoStunFakePredictionChance = 0;

    // Spear AutoStun (Spear -> Axe)
    public boolean spearAutoStunEnabled = true;
    public double spearAutoStunRange = 4.1;
    public int spearAutoStunDelay = 1;
    public int spearAutoStunFailChance = 0;
    public int spearAutoStunFakePredictionChance = 0;

    // Mace Swap (Any -> Mace for damage)
    public boolean maceSwapEnabled = true;
    public double maceSwapRange = 3.1;
    public int maceSwapDelay = 1;
    public int maceSwapFailChance = 0;
    public double maceSwapMinFallDistance = 3.0;

    // Reach Swap (Any -> Spear)
    public boolean spearReachSwapEnabled = true;
    public double spearReachSwapRange = 4.1;
    public double reachSwapActivationRange = 2.8;
    public int reachSwapBackDelay = 1;
    public boolean reachSwapIgnoreCobwebs = true;

    // Shared
    public boolean autoStunFacingCheck = true;
    public int axeToOriginalDelay = 1;
    public int maceToOriginalDelay = 1;


    // --- Minecart Tech ---
    public boolean tntMinecartPlacementEnabled = true;
    public boolean lavaCrossbowSequenceEnabled = true;
    public boolean bowSequenceEnabled = true;
    public int minFallDistance = 3;

    // --- Movement ---
    public boolean parkourEnabled = false;
    public double parkourPredict = 0.12;
    public double parkourMaxDropHeight = 0.6;

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
    public int windClutchMaxRetries = 2;
    public double windClutchSuccessVyDelta = 0.5;

    // --- Click Spam ---
    public boolean clickSpamEnabled = false;
    public int clickSpamCps = 12;
    public String clickSpamModifierKey = "key.keyboard.apostrophe";
    public boolean miningResetEnabled = false;
    public int miningResetChance = 100;
    public boolean miningResetSimulateStops = false;
    public double miningResetThreshold = 0.92;
    public int miningResetDelay = 0;
    public boolean waterDrainEnabled = false;
    public boolean waterDrainLavaEnabled = false;
    public boolean autoWaterDrainMode = false;
    public String autoWaterDrainHotkey = "key.keyboard.n";
    public int waterDrainSwitchToDelay = 0;
    public int waterDrainSwitchBackDelay = 0;
    public boolean autoExtinguishEnabled = false;
    public double autoExtinguishPitch = 60.0;

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
    public int triggerBotReactionMinDelay = 0;
    public int triggerBotReactionMaxDelay = 0;
    public boolean triggerBotWeaponOnly = true;
    public boolean attackOnCrit = false;
    public boolean triggerBotEnabled = true;
    public boolean triggerBotIncludePlayers = true;
    public boolean triggerBotExcludeTeammates = true;
    public boolean triggerBotIncludeHostiles = true;
    public boolean triggerBotIncludePassives = false;
    public boolean triggerBotExcludeVillagers = true;
    public boolean triggerBotIncludeCrystals = true;
    public boolean triggerBotActiveInInventory = false;
    public double triggerBotMaxRange = 3.0;
    public double triggerBotMinRange = 0.0;
    public boolean quickCrossbowEnabled = true;
    public int quickCrossbowReloadThreshold = 4;
    public transient boolean triggerBotToggledOn = true;

    // --- Hotkeys ---
    public String openSettingsHotkey = "key.keyboard.right.shift";
    public String lastCategory = "Attribute Swapping";
    public String masterToggleHotkey = "key.keyboard.m";
    public String teammateHotkey = "key.keyboard.g";
    public String triggerBotToggleHotkey = "key.keyboard.k";
    public String triggerBotHotkey = "key.keyboard.unknown";
    public String toggleOverlayHotkey = "key.keyboard.h";
    public String toggleESPHotkey = "key.keyboard.y";
    public String parkourHotkey = "key.keyboard.p";
    public String clutchHotkey = "key.keyboard.j";
    public String miningResetHotkey = "key.keyboard.unknown";
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
    public boolean showLatestToggleOverlay = false;
    public int overlayFontSize = 20;
    public int overlayBackgroundOpacity = 128;
    public String overlayAlignment = "Left";
    public String overlayVAlignment = "Top";
    public String overlayFontName = "Consolas";
    public boolean overlayLocked = false;

    // --- ESP Overlay ---
    public boolean showESP = false;
    public boolean espAntiVanish = true;
    public int espRefreshRate = 20; // FPS (approx)

    // --- Enemy Info ---
    public boolean showEnemyInfo = true;
    public boolean showHpDecimals = false;
    public boolean showLowestArmorPiece = false;
    public boolean doubleEnemyInfoRange = false;
    public boolean showBlastProtectionCount = false;

    // --- Potion Module ---
    public boolean potionModuleEnabled = false;
    public String potionHotkey = "key.keyboard.left.alt";
    public double potionActivationPitch = 60.0;
    public double potionHealthThreshold = 10.0;
    public boolean potionThrow = true;
    public boolean potionRestoreSlot = true;
    public double potionStrengthThreshold = 30.0;
    public double potionSpeedThreshold = 30.0;
    public double potionFireResThreshold = 30.0;


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
            // Update overlay config if it's running
            if (TutorialModClient.getInstance() != null && TutorialModClient.getOverlayManager() != null) {
                TutorialModClient.getOverlayManager().sendConfig();
            }
        } catch (IOException e) {
            TutorialMod.LOGGER.error("Error saving config", e);
        }
    }
}
