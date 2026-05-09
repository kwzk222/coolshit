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
    public int axeBoostedPredictionChanceEating = 0;
    public int axeBoostedPredictionChanceShieldDisabled = 0;
    public int eatingWindowTicks = 100;

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

    // Sprint Reset
    public boolean sprintResetEnabled = true;
    public String sprintResetMode = "Stop"; // "Stop" or "S-Tap"
    public int sprintResetDelay = 1;
    public int sprintResetCooldown = 0;

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
    public boolean autoCritEnabled = true;

    public boolean lungeSwapEnabled = true;
    public int lungeSwapBackDelay = 1;

    public boolean iceGhostSwapEnabled = false;
    public int iceGhostSwapDelay = 0;

    // --- Minecart Tech ---
    public boolean tntMinecartPlacementEnabled = true;
    public boolean tntMinecartPickblockAfterPlace = true;
    public boolean lavaCrossbowSequenceEnabled = true;
    public boolean bowSequenceEnabled = true;
    public int minFallDistance = 3;

    // --- Movement ---
    public boolean autoJumpEnabled = true;

    // Jump Reset
    public boolean jumpResetEnabled = true;
    public int jumpResetChance = 100;
    public int jumpResetDelay = 0;
    public boolean autoElytraFlyEnabled = true;
    public boolean parkourEnabled = false;
    public double parkourPredict = 0.0;
    public double parkourMaxDropHeight = 0.61;

    // --- Clutch ---
    public boolean clutchEnabled = true;
    public boolean waterClutchEnabled = true;
    public boolean windChargeClutchEnabled = true;
    public String clutchPriority = "Water"; // "Water" or "Wind Charge"
    public double clutchMinFallDistance = 3.0;
    public double clutchActivationPitch = 76.0;
    public int clutchRecoveryDelay = 5;
    public int clutchSwitchDelay = 0;
    public int clutchRestoreDelay = 0;
    public boolean clutchRestoreOriginalSlot = true;
    public boolean clutchAutoSwitch = true;

    // --- Click Spam ---
    public boolean clickSpamEnabled = true;
    public int clickSpamCps = 20;
    public String clickSpamModifierKey = "key.keyboard.6";
    public boolean miningResetEnabled = true;
    public int miningResetChance = 100;
    public boolean miningResetSimulateStops = true;
    public double miningResetThreshold = 0.92;
    public int miningResetDelay = 0;
    public boolean waterDrainEnabled = true;
    public boolean waterDrainLavaEnabled = false;
    public double lavaDrainMinPitch = 40.0;
    public boolean autoWaterDrainMode = true;
    public String autoWaterDrainHotkey = "key.keyboard.n";
    public int waterDrainSwitchToDelay = 0;
    public int waterDrainSwitchBackDelay = 0;
    public int bucketDrainPlaceDelay = 10;
    public int bucketDrainRestoreDelay = 2;
    public boolean bucketDrainFallbackEnabled = true;
    public boolean blockDrainFallbackEnabled = true;
    public boolean autoExtinguishEnabled = true;
    public double autoExtinguishPitch = 81.0;
    public int autoExtinguishFireTicksThreshold = 20;

    // --- General ---
    public boolean masterEnabled = true;
    public boolean bowReleaseBlockEnabled = true;
    public double bowAutoFireThreshold = 0.1;
    public int bowCooldown = 0;

    public boolean counterLavaDrainEnabled = true;
    public double counterLavaDrainRange = 5.0;
    public int counterLavaDrainSwitchDelay = 0;
    public int counterLavaDrainPickDelay = 0;
    public int counterLavaDrainRestoreDelay = 2;

    public boolean selfWaterWebEnabled = true;
    public int selfWaterWebSwitchDelay = 0;
    public int selfWaterWebPlaceDelay = 0;
    public int selfWaterWebPickDelay = 5;
    public int selfWaterWebRestoreDelay = 2;

    public boolean antiLavaFlowEnabled = true;
    public double antiLavaFlowEnemyRange = 6.0;
    public int antiLavaFlowPickDelay = 0;
    public int antiLavaFlowHoldDelay = 5;
    public int antiLavaFlowPlaceDelay = 0;
    public int antiLavaFlowRestoreDelay = 2;

    // --- Auto Tool ---
    public boolean autoToolSwitchEnabled = true;
    public boolean toolDurabilitySafetyEnabled = true;
    public boolean autoToolSwitchBackEnabled = true;
    public int autoToolSwitchBackMinDelay = 0;
    public int autoToolSwitchBackMaxDelay = 0;
    public int autoToolSwitchMineMinDelay = 0;
    public int autoToolSwitchMineMaxDelay = 0;

    // --- Auto Totem ---
    public boolean autoTotemEnabled = false;
    public boolean autoTotemSurvivalOnly = true;
    public boolean autoTotemRefillOnPop = true;
    public List<Integer> autoTotemHotbarSlots = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8));

    // --- TriggerBot ---
    public int triggerBotReactionMinDelay = 0;
    public int triggerBotReactionMaxDelay = 0;

    // --- Aim Assist ---
    public boolean aimAssistEnabled = false;
    public String aimAssistHotkey = "key.keyboard.1";
    public double aimAssistStrength = 2.14; // Slider will be adjusted to allow lower
    public double aimAssistFov = 113.0;
    public boolean aimAssistHorizontalOnly = true;
    public double aimAssistMaxRange = 4.3;
    public double aimAssistMinRange = 0.9;
    public boolean aimAssistWeaponOnly = true;
    public double aimAssistChargeThreshold = 0.85;
    public double aimAssistShieldStrength = 5.05;
    public double aimAssistShieldFov = 360.0;
    public double aimAssistShieldArc = 180.0;
    public double aimAssistAcceleration = 1.0;
    public double aimAssistDeceleration = 0.9;
    public double aimAssistEmaAlpha = 0.24;
    public int aimAssistDelay = 15;
    public boolean aimAssistPrediction = false;
    public double aimAssistPredictionFactor = 1.0;
    public double aimAssistBorderMin = -0.05;
    public double aimAssistBorderMax = -0.02;
    public boolean aimAssistIncludePlayers = true;
    public boolean aimAssistExcludeTeammates = true;
    public boolean aimAssistIncludeHostiles = true;
    public boolean aimAssistIncludePassives = false;
    public boolean aimAssistExcludeVillagers = true;

    public boolean triggerBotWeaponOnly = true;
    public boolean attackOnCrit = true;
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

    // --- Hotkeys ---
    public String openSettingsHotkey = "key.keyboard.right.shift";
    public String lastCategory = "Attribute Swapping";
    public String masterToggleHotkey = "";
    public String teammateHotkey = "key.keyboard.l";
    public String triggerBotHotkey = "key.keyboard.1";
    public String toggleOverlayHotkey = "key.keyboard.h";
    public String toggleESPHotkey = "key.keyboard.y";
    public String parkourHotkey = "key.keyboard.u";
    public String clutchHotkey = "key.keyboard.j";
    public String miningResetHotkey = "key.keyboard.6";
    public String sprintModeHotkey = "";
    public String sneakModeHotkey = "key.keyboard.n";
    public boolean activeInInventory = false;
    public boolean hotbarHoldCombat = false;
    public boolean creativeReachMatchSurvival = true;

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
    public boolean showLatestToggleOverlay = true;
    public boolean trajectoriesEnabled = true;
    public int trajectoriesColor = 0xFFFFFF;
    public int trajectoriesHitColor = 0xFF0000;
    public int overlayFontSize = 20;
    public int overlayBackgroundOpacity = 128;
    public String overlayAlignment = "Left";
    public String overlayVAlignment = "Top";
    public String overlayFontName = "Consolas";
    public boolean overlayLocked = false;

    // --- O-ESP ---
    public boolean showESP = false;
    public boolean espAntiVanish = true;
    public int espRefreshRate = 60; // FPS (approx)
    public double espScaleFactor = 1.0;
    public int espOffsetX = 0;
    public int espOffsetY = 0;
    public int espWidthAdjust = 0;
    public int espHeightAdjust = 0;
    public boolean espHideInMenus = true;
    public double espBoxScale = 1.0;
    public double espFovScale = 1.0;
    public double espAspectRatioScale = 1.0;
    public boolean espManualProjection = false;
    public double espManualFov = 70.0;
    public boolean espDebugMode = true;
    public boolean espShowArmor = true;
    public boolean espRelativeHealthColor = true;
    public int espColorHealthMore = 0x00FFFF;
    public int espColorHealthLess = 0xFFA500;

    // ESP Filters
    public boolean espPlayers = true;
    public boolean espVillagers = false;
    public boolean espHostiles = true;
    public boolean espPassives = false;
    public boolean espTamed = false;
    public double espMaxRange = 64.0;
    public double espMinRange = 0.0;

    // ESP Visuals
    public boolean espShowNamesPlayers = true;
    public boolean espShowNamesVillagers = true;
    public boolean espShowNamesHostiles = true;
    public boolean espShowNamesPassives = true;
    public boolean espShowNamesTamed = true;
    public boolean espShowDistance = true;
    public boolean espShowHealthBars = true;
    public double espHealthBarWidth = 0.1;
    public boolean espHealthBarInverted = false;
    public String espHealthBarSide = "Right";
    public int espHealthBarColorFull = 0x00FF00;
    public int espHealthBarColorMedium = 0xFFFF00;
    public int espHealthBarColorLow = 0xFF0000;
    public int espHealthBarColorEmpty = 0x000000;
    public int espArmorBarColorFull = 0x00FFFF;
    public int espArmorBarColorMedium = 0x55FFFF;
    public int espArmorBarColorLow = 0x00AAAA;
    public int espArmorBarColorEmpty = 0x000000;
    public boolean espShowStatusEffects = true;
    public double espDistanceHideThreshold = 0.0;
    public double espBoxWidthFactor = 0.22;
    public String espBoxMode = "3D";
    public int espColorTeammate = 0x00FF00;
    public int espColorEnemy = 0xFF0000;
    public int espColorVillager = 0xFF00FF;
    public int espColorHostile = 0xFFA500;
    public int espColorPassive = 0xFFFF00;
    public int espColorTamed = 0x0000FF;

    // X-Ray
    public boolean xrayEnabled = true;
    public boolean xrayFrustumCulling = false;
    public List<String> xrayBlocks = new ArrayList<>(Arrays.asList("minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"));
    public int xrayColor = 0x00FFFF;
    public int xrayRange = 32;
    public boolean xrayShowNames = false;
    public boolean xrayTextureMode = false;
    public int xrayTextureOpacity = 100;
    public double xrayTextureScale = 0.8;
    public boolean xrayClumpingEnabled = true;
    public boolean xray26Adjacent = true;

    // --- O-ESP ---
    public boolean espFrustumCulling = true;

    // --- Potion Module ---
    public boolean potionModuleEnabled = false;
    public String potionHotkey = "key.keyboard.left.alt";
    public double potionActivationPitch = 60.0;
    public double potionHealthThreshold = 10.0;
    public boolean potionTurtleMasterEnabled = true;
    public double potionTurtleMasterHealthThreshold = 6.0;
    public boolean potionTurtleMasterPriority = true;
    public boolean potionThrow = true;
    public boolean potionRestoreSlot = true;
    public double potionStrengthThreshold = 30.0;
    public double potionSpeedThreshold = 30.0;
    public double potionFireResThreshold = 30.0;
    public double potionRegenThreshold = 30.0;


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
