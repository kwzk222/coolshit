package net.rev.tutorialmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.block.AbstractRailBlock;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.BowItem;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.block.Blocks;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.lwjgl.glfw.GLFW;
import net.rev.tutorialmod.event.AttackEntityCallback;
import net.rev.tutorialmod.mixin.GameOptionsAccessor;
import net.rev.tutorialmod.mixin.MinecraftClientAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.rev.tutorialmod.modules.AimAssist;
import net.rev.tutorialmod.modules.AutoTotem;
import net.rev.tutorialmod.modules.ESPModule;
import net.rev.tutorialmod.modules.ESPOverlayManager;
import net.rev.tutorialmod.modules.OverlayManager;
import net.rev.tutorialmod.modules.PotionModule;
import net.rev.tutorialmod.modules.TriggerBot;
import net.rev.tutorialmod.modules.misc.ClickSpamModule;
import net.rev.tutorialmod.modules.misc.TrajectoriesModule;
import net.rev.tutorialmod.modules.movement.ClutchModule;
import net.rev.tutorialmod.modules.movement.ParkourModule;

public class TutorialModClient implements ClientModInitializer {

    private static final Random RANDOM = new Random();
    private final Map<String, Boolean> wasMacroKeyPressed = new HashMap<>();

    // --- Singleton Instance ---
    private static TutorialModClient instance;

    public static TutorialModClient getInstance() {
        return instance;
    }

    // --- Modules & Features ---
    private TriggerBot triggerBot;
    private AimAssist aimAssist;
    private AutoTotem autoTotem;
    private PotionModule potionModule;
    private ParkourModule parkourModule;
    private ClutchModule clutchModule;
    private ESPModule espModule;
    private TrajectoriesModule trajectoriesModule;
    private static OverlayManager overlayManager;
    private static ESPOverlayManager espOverlayManager;

    public AutoTotem getAutoTotem() {
        return autoTotem;
    }

    public static OverlayManager getOverlayManager() {
        return overlayManager;
    }

    public AimAssist getAimAssist() {
        return aimAssist;
    }

    public ESPModule getESPModule() {
        return espModule;
    }

    public TrajectoriesModule getTrajectoriesModule() {
        return trajectoriesModule;
    }

    public static ESPOverlayManager getESPOverlayManager() {
        return espOverlayManager;
    }


    // --- Keybind States ---
    private boolean openSettingsWasPressed = false;
    private boolean masterToggleWasPressed = false;
    private boolean teammateWasPressed = false;
    private boolean overlayToggleWasPressed = false;
    private boolean espToggleWasPressed = false;
    private boolean parkourToggleWasPressed = false;
    private boolean clutchToggleWasPressed = false;
    private boolean miningResetWasPressed = false;
    private boolean sprintModeWasPressed = false;
    private boolean sneakModeWasPressed = false;
    private boolean autoWaterDrainModeWasPressed = false;
    private boolean aimAssistToggleWasPressed = false;

    // --- State: Combat Swap ---
    private boolean isExecutingCombo = false;
    private int sprintResetTimer = -1;
    private int sprintResetCooldownTimer = -1;
    private int comboRestoreTicks = -1;
    private int comboRestoreSlot = -1;

    private int iceGhostSwapTicks = -1;
    private int iceGhostRestoreSlot = -1;

    // --- State: Placement Sequence (TNT Minecart, etc.) ---
    private enum PlacementAction { NONE, PLACE_TNT_MINECART, AWAITING_UTILITY_USE, SWITCH_TO_CROSSBOW, SWITCH_TO_BOW }
    private int placementCooldown = -1;
    private boolean isMinecartSynced = false;
    private PlacementAction nextPlacementAction = PlacementAction.NONE;
    private BlockPos railPos = null;
    private int utilitySlot = -1;
    private int crossbowSlot = -1;
    private int actionTimeout = -1;

    // --- State: Misc ---
    public static long lastBowShotTick = -1;
    public static int awaitingRailConfirmationCooldown = -1;
    public static int awaitingMinecartConfirmationCooldown = -1;

    private int pointingTickCounter = 0;
    private String lastLongCoordsInfo = null;

    private final Map<Integer, Long> shieldCooldowns = new HashMap<>();
    private final Map<Integer, Long> eatingEndTicks = new HashMap<>();

    private int originalSlotBeforeDrain = -1;
    private int drainRestoreTicks = -1;

    private boolean lastScreenWasNull = true;
    private int espSyncBurstTicks = 0;
    private int drainPendingSlot = -1;
    private int drainSwitchToTicks = -1;
    private int drainSwitchBackTimer = -1;

    private int autoCritTimer = -1;
    private Entity autoCritTarget = null;
    private int autoCritDelay = -1;
    private boolean isAutoCritAttacking = false;

    private int secondAxeHitTimer = -1;
    private Entity secondAxeHitTarget = null;

    private boolean isLungeSwapping = false;
    private long lastPlacedWaterTick = -1;

    private enum WebWaterState { NONE, SWITCH_TO_BUCKET, PLACING, PICKING_UP, RESTORING }
    private WebWaterState currentWebWaterState = WebWaterState.NONE;
    private int webWaterStateTimer = -1;
    private int originalSlotBeforeWebWater = -1;
    private int selfWaterWebCooldown = -1;

    private enum AntiLavaFlowState { NONE, SWITCH_TO_EMPTY, PICKING_UP, HOLDING, PLACING, RESTORING }
    private AntiLavaFlowState currentAntiLavaFlowState = AntiLavaFlowState.NONE;
    private int antiLavaFlowTimer = -1;
    private int originalSlotBeforeAntiLava = -1;

    private enum CounterLavaState { NONE, SWITCH_TO_EMPTY, PICKING_UP, RESTORING }
    private CounterLavaState currentCounterLavaState = CounterLavaState.NONE;
    private int counterLavaTimer = -1;
    private int originalSlotBeforeCounterLava = -1;

    private enum ExtinguishState { NONE, PUNCHING, SWITCH_TO_BUCKET, PLACING, PICKING_UP, SWITCHING_BACK }
    private ExtinguishState currentExtinguishState = ExtinguishState.NONE;
    private int extinguishTimer = -1;
    private int originalSlotBeforeExtinguish = -1;

    private enum FallbackDrainState { NONE, START, PLACING, PICKING_UP, RESTORING }
    private FallbackDrainState currentFallbackDrainState = FallbackDrainState.NONE;
    private int fallbackDrainTimer = -1;
    private int originalSlotBeforeFallback = -1;
    private BlockPos fallbackDrainTargetPos = null;
    private Direction fallbackDrainTargetSide = null;
    private int fallbackDrainPendingSlot = -1;
    private boolean isBlockFallback = false;

    private int crossbowUseTicks = 0;
    private boolean crossbowWasUsing = false;

    private String overlayStatusMessage = null;
    private long overlayStatusTime = 0;

    private boolean isWaitingForBowRelease = false;
    private boolean isAutoReleasingBow = false;
    private int elytraFlyTimer = -1;
    private boolean wasEating = false;
    private boolean ignoreNextUse = false;
    private long lastBlockPlaceTick = -1;

    public void setPendingBowRelease(boolean val) {
        this.isWaitingForBowRelease = val;
    }

    public boolean isAutoReleasingBow() {
        return this.isAutoReleasingBow;
    }


    public void onShieldBreak(int entityId) {
        if (MinecraftClient.getInstance().world != null) {
            shieldCooldowns.put(entityId, MinecraftClient.getInstance().world.getTime() + 100);
        }
    }

    public void onEntityFinishEating(int entityId) {
        if (MinecraftClient.getInstance().world != null) {
            eatingEndTicks.put(entityId, MinecraftClient.getInstance().world.getTime() + TutorialMod.CONFIG.eatingWindowTicks);
        }
    }

    public void clearShieldCooldowns() {
        shieldCooldowns.clear();
        eatingEndTicks.clear();
    }

    private boolean isShieldCooldown(Entity entity) {
        if (MinecraftClient.getInstance().world == null) return false;
        Long expiry = shieldCooldowns.get(entity.getId());
        if (expiry == null) return false;
        if (MinecraftClient.getInstance().world.getTime() > expiry) {
            shieldCooldowns.remove(entity.getId());
            return false;
        }
        return true;
    }

    private boolean isEatingBoostActive(Entity entity) {
        if (MinecraftClient.getInstance().world == null) return false;
        Long expiry = eatingEndTicks.get(entity.getId());
        if (expiry == null) return false;
        if (MinecraftClient.getInstance().world.getTime() > expiry) {
            eatingEndTicks.remove(entity.getId());
            return false;
        }
        return true;
    }

    public void setOverlayStatus(String message) {
        if (message == null) return;

        String transformed = message;
        if (message.contains(" set to ")) {
            String[] parts = message.split(" set to ");
            String feature = parts[0];
            String status = parts[1];
            if (status.equalsIgnoreCase("ON")) {
                transformed = "Enabled " + feature;
            } else if (status.equalsIgnoreCase("OFF")) {
                transformed = "Disabled " + feature;
            } else {
                transformed = feature + ": " + status;
            }
        }

        this.overlayStatusMessage = transformed;
        this.overlayStatusTime = System.currentTimeMillis();
    }


    @Override
    public void onInitializeClient() {
        instance = this;
        triggerBot = new TriggerBot();
        aimAssist = new AimAssist();
        autoTotem = new AutoTotem();
        potionModule = new PotionModule();
        parkourModule = new ParkourModule();
        clutchModule = new ClutchModule();
        espModule = new ESPModule();
        trajectoriesModule = new TrajectoriesModule();
        overlayManager = new OverlayManager();
        espOverlayManager = new ESPOverlayManager();
        autoTotem.init();
        parkourModule.init();

        // Add shutdown hook to stop overlay process
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            overlayManager.stop();
            espOverlayManager.stop();
        }));

        // Register Event Listeners
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        AttackEntityCallback.EVENT.register(this::onAttackEntity);

        // Register Commands
        new CommandManager().registerCommands();

        // Chat (public chat) modify event
        ClientSendMessageEvents.MODIFY_CHAT.register(message -> {
            ModConfig cfg = TutorialMod.CONFIG;
            if (!cfg.replaceInChat) return message;
            if (message == null) return message;

            String trigger = cfg.caseSensitive ? cfg.trigger : cfg.trigger.toLowerCase();
            String check = cfg.caseSensitive ? message : message.toLowerCase();

            // exact equality check for chat messages
            if (check.equals(trigger)) {
                return formatCoords(cfg);
            }

            return message;
        });

        // Command modify event: modifies the command string (without leading '/')
        ClientSendMessageEvents.MODIFY_COMMAND.register(command -> {
            ModConfig cfg = TutorialMod.CONFIG;
            if (!cfg.replaceInCommands) return command;
            if (command == null) return command;

            // build regex that only replaces whole-word occurrences of the trigger
            int flags = cfg.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            Pattern p = Pattern.compile("\\b" + Pattern.quote(cfg.trigger) + "\\b", flags);
            Matcher m = p.matcher(command);
            if (m.find()) {
                String coords = formatCoords(cfg);
                // safe replace all (quote replacement in case coords contain $ or \)
                return m.replaceAll(Matcher.quoteReplacement(coords));
            }
            return command;
        });

    }

    private void onClientTick(MinecraftClient client) {
        if (sprintResetTimer > 0) {
            sprintResetTimer--;
        } else if (sprintResetTimer == 0) {
            sprintResetTimer = -1;
        }

        if (sprintResetCooldownTimer > 0) {
            sprintResetCooldownTimer--;
        }

        // Handle keybinds first, as they might toggle features on/off.
        handleKeybinds(client);
        handleQuickCrossbow(client);
        handleAutoBowRelease(client);

        if (client.world != null && client.world.getTime() % 20 == 0) {
            shieldCooldowns.entrySet().removeIf(entry -> client.world.getTime() > entry.getValue());
            eatingEndTicks.entrySet().removeIf(entry -> client.world.getTime() > entry.getValue());
        }

        handleChatMacros(client);

        // Handle TriggerBot separately, as it may have its own master toggle.
        if (triggerBot != null) {
            triggerBot.onTick();
        }

        if (aimAssist != null) {
            aimAssist.onTick();
        }

        handleSecondAxeHit(client);
        handleComboRestore(client);
        handleIceGhostSwapTick(client);

        // --- Centralized Overlay Logic ---
        boolean shouldOverlayBeRunning = TutorialMod.CONFIG.showCoordsOverlay;
        if (shouldOverlayBeRunning && !overlayManager.isRunning() && client.player != null) {
            overlayManager.start();
        } else if ((!shouldOverlayBeRunning || client.player == null) && overlayManager.isRunning()) {
            overlayManager.stop();
        }

        // --- ESP Overlay Logic ---
        boolean shouldESPBeRunning = TutorialMod.CONFIG.showESP;
        if (shouldESPBeRunning && !espOverlayManager.isRunning() && client.player != null) {
            espOverlayManager.start();
        } else if ((!shouldESPBeRunning || client.player == null) && espOverlayManager.isRunning()) {
            espOverlayManager.stop();
        }

        boolean screenChanged = (client.currentScreen == null) != lastScreenWasNull;
        if (screenChanged) {
            espModule.triggerSync();
            lastScreenWasNull = (client.currentScreen == null);
            // If we just unpaused (screen became null), start a small sync burst
            if (client.currentScreen == null) {
                espSyncBurstTicks = 20; // 1 second at 20tps
            }
        }

        if (espSyncBurstTicks > 0) {
            espModule.triggerSync();
            espSyncBurstTicks--;
        }


        if (overlayManager.isRunning() && client.player != null) {
            if (TutorialMod.CONFIG.showCoordsOverlay) {
                overlayManager.update(formatCoordsForOverlay(client));
            } else {
                overlayManager.update(""); // Clear/Hide overlay
            }
        }


        // Handle AutoToolSwitch tick
        TutorialMod.getAutoToolSwitch().onTick();

        // --- Feature Ticks ---
        // These are handled even if master is disabled to ensure state is cleaned up or updated.
        handlePlacementSequence(client);
        handleConfirmationCooldowns(client);

        // Master toggle check for all subsequent features.
        if (!TutorialMod.CONFIG.masterEnabled) return;

        if (TutorialMod.CONFIG.autoTotemEnabled) {
            autoTotem.onTick(client);
        }

        if (clutchModule != null) {
            clutchModule.tick();
        }

        if (potionModule != null) {
            potionModule.onTick(client);
        }

        handleAutoWaterDrain(client);
        handleAutoExtinguish(client);
        handleWaterDrainSwitchTo(client);
        handleWaterDrainRestore(client);
        handleSelfWaterWeb(client);
        handleCounterLavaDrain(client);
        handleAntiLavaFlow(client);
        handleFallbackDrainTick(client);
        handleAutoCrit(client);
        handleAutoElytraFly(client);

        if (client.player != null) {
            boolean isEating = client.player.isUsingItem() && client.player.getActiveItem().getComponents().contains(net.minecraft.component.DataComponentTypes.FOOD);
            if (wasEating && !isEating && isKeyDown(client.options.useKey.getBoundKeyTranslationKey())) {
                ignoreNextUse = true;
            }
            if (ignoreNextUse && !isKeyDown(client.options.useKey.getBoundKeyTranslationKey())) {
                ignoreNextUse = false;
            }
            wasEating = isEating;
        }

        ClickSpamModule.onTick();
    }

    private void handleAutoBowRelease(MinecraftClient client) {
        if (client.player == null) return;

        ItemStack stack = client.player.getActiveItem();
        boolean isBow = stack.getItem() instanceof BowItem;

        // Detect release: using bow, key NOT pressed, not yet flagged
        if (isBow && !client.options.useKey.isPressed() && !isWaitingForBowRelease) {
            int useTicks = client.player.getItemUseTime();
            float progress = BowItem.getPullProgress(useTicks);
            if (progress < 1.0f) {
                isWaitingForBowRelease = true;
            }
        }

        // Reset waiting if we switch items or stop using (e.g. forced stop)
        if (!isBow || !client.player.isUsingItem()) {
            isWaitingForBowRelease = false;
        }

        // If player physically holds the key again, cancel waiting
        if (client.options.useKey.isPressed()) {
            isWaitingForBowRelease = false;
        }

        if (isWaitingForBowRelease) {
            int useTicks = client.player.getItemUseTime();
            float progress = BowItem.getPullProgress(useTicks);

            if (progress >= TutorialMod.CONFIG.bowAutoFireThreshold) {
                setOverlayStatus("Auto-Firing Bow");
                isAutoReleasingBow = true;
                if (client.interactionManager != null) {
                    client.interactionManager.stopUsingItem(client.player);
                }
                isAutoReleasingBow = false;
                isWaitingForBowRelease = false;
            }
        }
    }


    private void handleAutoElytraFly(MinecraftClient client) {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.autoElytraFlyEnabled) {
            elytraFlyTimer = -1;
            return;
        }
        if (client.player == null) return;

        if (elytraFlyTimer > 0) {
            elytraFlyTimer--;
            if (client.player.getEquippedStack(EquipmentSlot.CHEST).isOf(Items.ELYTRA) && !client.player.isOnGround()) {
                client.player.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(client.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                elytraFlyTimer = -1;
            }
        } else if (elytraFlyTimer == 0) {
            elytraFlyTimer = -1;
        }
    }


    private void handleAutoCrit(MinecraftClient client) {
        if (autoCritDelay > 0) {
            autoCritDelay--;
        } else if (autoCritDelay == 0) {
            if (autoCritTarget != null && client.interactionManager != null && client.player != null) {
                isAutoCritAttacking = true;
                client.interactionManager.attackEntity(client.player, autoCritTarget);
                client.player.swingHand(Hand.MAIN_HAND);
                isAutoCritAttacking = false;
            }
            autoCritDelay = -1;
            autoCritTarget = null;
        }
    }


    private ActionResult onAttackEntity(PlayerEntity player, Entity target) {
        if (!TutorialMod.CONFIG.masterEnabled || isExecutingCombo || isLungeSwapping) return ActionResult.PASS;

        MinecraftClient mc = MinecraftClient.getInstance();

        if (isAutoCritAttacking) return ActionResult.PASS;

        if (TutorialMod.CONFIG.autoCritEnabled && player.fallDistance > 0 && player.isSprinting() &&
            isKeyDown(mc.options.forwardKey.getBoundKeyTranslationKey()) &&
            isKeyDown(mc.options.jumpKey.getBoundKeyTranslationKey())) {

            autoCritTarget = target;
            autoCritDelay = 1;
            autoCritTimer = 2;
            return ActionResult.FAIL;
        }

        if (!(target instanceof PlayerEntity attackedPlayer)) return ActionResult.PASS;

        double dist = player.distanceTo(attackedPlayer);
        boolean isShielding = attackedPlayer.isUsingItem() && attackedPlayer.getActiveItem().isOf(Items.SHIELD);
        boolean hasArmor = isArmored(attackedPlayer);

        // Check if any combo should be triggered
        boolean needsSpear = dist > TutorialMod.CONFIG.reachSwapActivationRange && TutorialMod.CONFIG.spearReachSwapEnabled && findSpearInHotbar(player, false) != -1;

        // Determine if stun is needed
        boolean isFacing = true;
        if (TutorialMod.CONFIG.autoStunFacingCheck) {
            Vec3d targetLook = attackedPlayer.getRotationVector();
            Vec3d selfPos = new Vec3d(player.getX(), player.getY(), player.getZ());
            Vec3d targetPos = new Vec3d(attackedPlayer.getX(), attackedPlayer.getY(), attackedPlayer.getZ());
            Vec3d toSelf = selfPos.subtract(targetPos).normalize();
            isFacing = toSelf.dotProduct(targetLook) > 0;
        }

        ItemStack held = player.getMainHandStack();
        boolean predictShield = false;
        if (!isShielding && isFacing && (attackedPlayer.getMainHandStack().isOf(Items.SHIELD) || attackedPlayer.getOffHandStack().isOf(Items.SHIELD)) && !isShieldCooldown(attackedPlayer)) {
            // Check if looking at us
            Vec3d dirToMe = new Vec3d(player.getX(), player.getY(), player.getZ()).subtract(attackedPlayer.getX(), attackedPlayer.getY(), attackedPlayer.getZ()).normalize();
            if (attackedPlayer.getRotationVector().dotProduct(dirToMe) > 0.8) {
                int chance;
                if (held.isIn(ItemTags.SPEARS) || held.isOf(Items.TRIDENT)) chance = TutorialMod.CONFIG.spearAutoStunFakePredictionChance;
                else if (held.getItem() == Items.MACE) chance = TutorialMod.CONFIG.maceAutoStunFakePredictionChance;
                else chance = TutorialMod.CONFIG.axeSwapFakePredictionChance;

                if (isEatingBoostActive(attackedPlayer)) {
                    chance = Math.max(chance, TutorialMod.CONFIG.axeBoostedPredictionChanceEating);
                }
                if (isShieldCooldown(player)) { // user's shield disabled
                    chance = Math.max(chance, TutorialMod.CONFIG.axeBoostedPredictionChanceShieldDisabled);
                }

                if (chance > 0 && RANDOM.nextInt(100) < chance) {
                    predictShield = true;
                }
            }
        }

        boolean needsStun = (isShielding || predictShield) && isFacing && findAxeInHotbar(player) != -1;
        // Check specific stun toggles based on held item
        if (held.isIn(ItemTags.SPEARS) || held.isOf(Items.TRIDENT)) {
            if (!TutorialMod.CONFIG.spearAutoStunEnabled) needsStun = false;
        } else if (held.getItem() == Items.MACE) {
            if (!TutorialMod.CONFIG.maceAutoStunEnabled) needsStun = false;
        } else {
            if (!TutorialMod.CONFIG.axeSwapEnabled) needsStun = false;
        }

        boolean needsMace = hasArmor && player.fallDistance > TutorialMod.CONFIG.maceSwapMinFallDistance && TutorialMod.CONFIG.maceSwapEnabled && findMaceInHotbar(player) != -1;

        if (needsSpear || needsStun || needsMace) {
            executeCombatCombo(player, attackedPlayer, false);
            return ActionResult.FAIL;
        }

        if (player.isSprinting()) {
            triggerSprintReset();
        }

        return ActionResult.PASS;
    }

    private void executeCombatCombo(PlayerEntity player, PlayerEntity target, boolean forceSpear) {
        if (isExecutingCombo || player == null || target == null) return;
        isExecutingCombo = true;

        if (player.isSprinting()) {
            triggerSprintReset();
        }
        int originalSlot = ((PlayerInventoryMixin) player.getInventory()).getSelectedSlot();
        MinecraftClient client = MinecraftClient.getInstance();

        int delay = 0;

        try {
            double dist = player.distanceTo(target);
            boolean alreadyAttackedInCombo = false;

            // 1. Spear Hit
            if (forceSpear || (dist > TutorialMod.CONFIG.reachSwapActivationRange && dist <= TutorialMod.CONFIG.spearReachSwapRange && TutorialMod.CONFIG.spearReachSwapEnabled)) {
                int spearSlot = findSpearInHotbar(player, false);
                if (spearSlot != -1) {
                    syncSlot(spearSlot);
                    if (client.interactionManager != null) {
                        client.interactionManager.attackEntity(player, target);
                        player.swingHand(Hand.MAIN_HAND);
                        alreadyAttackedInCombo = true;
                    }
                }
            }

            // Re-check shielding after spear hit (unlikely to change in same tick but good for logic)
            boolean isShielding = target.isUsingItem() && target.getActiveItem().isOf(Items.SHIELD);
            boolean isFacing = true;
            if (TutorialMod.CONFIG.autoStunFacingCheck) {
                Vec3d targetLook = target.getRotationVector();
                Vec3d selfPos = new Vec3d(player.getX(), player.getY(), player.getZ());
                Vec3d targetPos = new Vec3d(target.getX(), target.getY(), target.getZ());
                Vec3d toSelf = selfPos.subtract(targetPos).normalize();
                isFacing = toSelf.dotProduct(targetLook) > 0;
            }

            // 2. Axe Stun (if shielding or predicted)
            boolean predictShield = false;
            if (!isShielding && isFacing && (target.getMainHandStack().isOf(Items.SHIELD) || target.getOffHandStack().isOf(Items.SHIELD)) && !isShieldCooldown(target)) {
                Vec3d dirToMe = new Vec3d(player.getX(), player.getY(), player.getZ()).subtract(target.getX(), target.getY(), target.getZ()).normalize();
                if (target.getRotationVector().dotProduct(dirToMe) > 0.8) {
                    int chance;
                    ItemStack held = player.getMainHandStack();
                    if (held.isIn(ItemTags.SPEARS) || held.isOf(Items.TRIDENT)) chance = TutorialMod.CONFIG.spearAutoStunFakePredictionChance;
                    else if (held.getItem() == Items.MACE) chance = TutorialMod.CONFIG.maceAutoStunFakePredictionChance;
                    else chance = TutorialMod.CONFIG.axeSwapFakePredictionChance;

                    if (isEatingBoostActive(target)) {
                        chance = Math.max(chance, TutorialMod.CONFIG.axeBoostedPredictionChanceEating);
                    }
                    if (isShieldCooldown(player)) {
                        chance = Math.max(chance, TutorialMod.CONFIG.axeBoostedPredictionChanceShieldDisabled);
                    }

                    if (chance > 0 && RANDOM.nextInt(100) < chance) predictShield = true;
                }
            }

            if ((isShielding || predictShield) && isFacing) {
                int axeSlot = findAxeInHotbar(player);
                if (axeSlot != -1) {
                    // If we haven't attacked yet (no spear hit), and we were holding a sword, do the initial sword hit
                    if (!alreadyAttackedInCombo && player.getInventory().getStack(originalSlot).isIn(ItemTags.SWORDS) && dist <= 3.1) {
                        if (client.interactionManager != null) {
                            client.interactionManager.attackEntity(player, target);
                            player.swingHand(Hand.MAIN_HAND);
                        }
                    }

                    syncSlot(axeSlot);
                    if (client.interactionManager != null) {
                        // First Axe Hit
                        client.interactionManager.attackEntity(player, target);
                        player.swingHand(Hand.MAIN_HAND);

                        // Second Axe Hit (Double Click simulation)
                        // Setting a 1-tick delay for the second hit to ensure it registers properly on most servers
                        this.secondAxeHitTarget = target;
                        this.secondAxeHitTimer = 1;
                        alreadyAttackedInCombo = true;
                    }
                }
            }

            // 3. Mace Hit (if falling and armored)
            if (isArmored(target) && player.fallDistance > TutorialMod.CONFIG.maceSwapMinFallDistance && TutorialMod.CONFIG.maceSwapEnabled) {
                int maceSlot = findMaceInHotbar(player);
                if (maceSlot != -1) {
                    syncSlot(maceSlot);
                    if (client.interactionManager != null) {
                        client.interactionManager.attackEntity(player, target);
                        player.swingHand(Hand.MAIN_HAND);
                        alreadyAttackedInCombo = true;
                    }
                }
            }

            // Restore original slot with delay to ensure visual animation
            delay = Math.max(TutorialMod.CONFIG.axeToOriginalDelay, TutorialMod.CONFIG.maceToOriginalDelay);
            // If we have a pending second axe hit, we must not restore yet
            if (secondAxeHitTimer > 0) {
                 this.comboRestoreSlot = originalSlot;
                 this.comboRestoreTicks = delay + secondAxeHitTimer;
            } else {
                if (delay <= 0) {
                    syncSlot(originalSlot);
                } else {
                    this.comboRestoreSlot = originalSlot;
                    this.comboRestoreTicks = delay;
                }
            }
        } finally {
            isExecutingCombo = false;
        }
    }

    private void handleSecondAxeHit(MinecraftClient client) {
        if (secondAxeHitTimer > 0) {
            secondAxeHitTimer--;
            if (secondAxeHitTimer == 0) {
                if (secondAxeHitTarget != null && client.player != null && client.interactionManager != null) {
                    // Verify we are still holding an axe
                    if (client.player.getMainHandStack().getItem() instanceof AxeItem) {
                        client.interactionManager.attackEntity(client.player, secondAxeHitTarget);
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                }
                secondAxeHitTarget = null;
                secondAxeHitTimer = -1;
            }
        }
    }

    private void handleComboRestore(MinecraftClient client) {
        if (comboRestoreTicks > 0) {
            comboRestoreTicks--;
        } else if (comboRestoreTicks == 0) {
            if (comboRestoreSlot != -1) {
                syncSlot(comboRestoreSlot);
            }
            comboRestoreTicks = -1;
            comboRestoreSlot = -1;
        }
    }


    private void syncSlot(int slot) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        ((PlayerInventoryMixin) client.player.getInventory()).setSelectedSlot(slot);
        if (client.interactionManager != null) {
            ((net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor) client.interactionManager).invokeSyncSelectedSlot();
        }
    }

    public void pickblockTntMinecart(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        // Requirement: Only pickblock if looking at a minecart and in range
        if (!(client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr)) return;
        if (ehr.getEntity().getType() != net.minecraft.entity.EntityType.TNT_MINECART) return;
        if (client.player.squaredDistanceTo(ehr.getEntity()) > 25.0) return;

        int tntSlot = -1;
        // Search hotbar first
        for (int i = 0; i < 9; i++) {
            if (client.player.getInventory().getStack(i).isOf(Items.TNT_MINECART)) {
                tntSlot = i;
                break;
            }
        }
        if (tntSlot != -1) {
            syncSlot(tntSlot);
            return;
        }
        // Search main inventory
        for (int i = 9; i < 36; i++) {
            if (client.player.getInventory().getStack(i).isOf(Items.TNT_MINECART)) {
                tntSlot = i;
                break;
            }
        }
        if (tntSlot != -1) {
            int emptyHotbar = -1;
            for (int i = 0; i < 9; i++) {
                if (client.player.getInventory().getStack(i).isEmpty()) {
                    emptyHotbar = i;
                    break;
                }
            }

            if (emptyHotbar != -1) {
                client.interactionManager.clickSlot(0, tntSlot, emptyHotbar, net.minecraft.screen.slot.SlotActionType.SWAP, client.player);
                syncSlot(emptyHotbar);
            } else {
                int current = ((PlayerInventoryMixin)client.player.getInventory()).getSelectedSlot();
                client.interactionManager.clickSlot(0, tntSlot, current, net.minecraft.screen.slot.SlotActionType.SWAP, client.player);
                syncSlot(current);
            }
        }
    }

    private void handleKeybinds(MinecraftClient client) {
        if (client.player == null) return;

        // F3 Safety Check
        try {
            if (InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_F3)) {
                return;
            }
        } catch (Exception ignored) {}

        if (!TutorialMod.CONFIG.activeInInventory && client.currentScreen != null) {
            return;
        }

        // --- Toggle Auto Water Drain Mode Hotkey ---
        boolean isAutoWaterDrainModePressed = isKeyDown(TutorialMod.CONFIG.autoWaterDrainHotkey);
        if (isAutoWaterDrainModePressed && !autoWaterDrainModeWasPressed) {
            TutorialMod.CONFIG.autoWaterDrainMode = !TutorialMod.CONFIG.autoWaterDrainMode;
            TutorialMod.CONFIG.save();
            TutorialMod.sendUpdateMessage("Auto Bucket Drain Mode set to " + (TutorialMod.CONFIG.autoWaterDrainMode ? "ON" : "OFF"));
        }
        autoWaterDrainModeWasPressed = isAutoWaterDrainModePressed;

        // --- Open Settings Hotkey ---
        boolean isOpenSettingsPressed = isKeyDown(TutorialMod.CONFIG.openSettingsHotkey);
        if (isOpenSettingsPressed && !openSettingsWasPressed) {
            client.setScreen(new ModMenuIntegration().getModConfigScreenFactory().create(client.currentScreen));
        }
        openSettingsWasPressed = isOpenSettingsPressed;


        // --- Toggle Clutch Hotkey ---
        boolean isClutchTogglePressed = isKeyDown(TutorialMod.CONFIG.clutchHotkey);
        if (isClutchTogglePressed && !clutchToggleWasPressed) {
            TutorialMod.CONFIG.clutchEnabled = !TutorialMod.CONFIG.clutchEnabled;
            TutorialMod.CONFIG.save();
            TutorialMod.sendUpdateMessage("Clutch set to " + (TutorialMod.CONFIG.clutchEnabled ? "ON" : "OFF"));
        }
        clutchToggleWasPressed = isClutchTogglePressed;

        // Aim Assist is now strictly hold-to-activate, handled in AimAssist.java

        boolean isMasterTogglePressed = isKeyDown(TutorialMod.CONFIG.masterToggleHotkey);
        if (isMasterTogglePressed && !masterToggleWasPressed) {
            TutorialMod.CONFIG.masterEnabled = !TutorialMod.CONFIG.masterEnabled;
            TutorialMod.CONFIG.save();
            TutorialMod.sendUpdateMessage("Master Switch set to " + (TutorialMod.CONFIG.masterEnabled ? "ON" : "OFF"));
        }
        masterToggleWasPressed = isMasterTogglePressed;


        boolean isTeammateKeyPressed = isKeyDown(TutorialMod.CONFIG.teammateHotkey);
        if (isTeammateKeyPressed && !teammateWasPressed) {
            handleTeammateKeybind(client);
        }
        teammateWasPressed = isTeammateKeyPressed;

        // --- Toggle Overlay Hotkey ---
        boolean isToggleOverlayPressed = isKeyDown(TutorialMod.CONFIG.toggleOverlayHotkey);
        if (isToggleOverlayPressed && !overlayToggleWasPressed) {
            TutorialMod.CONFIG.showCoordsOverlay = !TutorialMod.CONFIG.showCoordsOverlay;
            TutorialMod.CONFIG.save();
            TutorialMod.sendUpdateMessage("Coords Overlay set to " + (TutorialMod.CONFIG.showCoordsOverlay ? "ON" : "OFF"));
            if (TutorialMod.CONFIG.showCoordsOverlay) {
                getOverlayManager().start();
            } else {
                getOverlayManager().stop();
            }
        }
        overlayToggleWasPressed = isToggleOverlayPressed;

        // --- Toggle ESP Hotkey ---
        boolean isToggleESPPressed = isKeyDown(TutorialMod.CONFIG.toggleESPHotkey);
        if (isToggleESPPressed && !espToggleWasPressed) {
            TutorialMod.CONFIG.showESP = !TutorialMod.CONFIG.showESP;
            TutorialMod.CONFIG.save();
            TutorialMod.sendUpdateMessage("O-ESP set to " + (TutorialMod.CONFIG.showESP ? "ON" : "OFF"));
            if (TutorialMod.CONFIG.showESP) {
                espOverlayManager.start();
            } else {
                espOverlayManager.stop();
            }
        }
        espToggleWasPressed = isToggleESPPressed;

        // --- Toggle Parkour Hotkey ---
        boolean isParkourTogglePressed = isKeyDown(TutorialMod.CONFIG.parkourHotkey);
        if (isParkourTogglePressed && !parkourToggleWasPressed) {
            parkourModule.toggle();
            TutorialMod.sendUpdateMessage("Parkour set to " + (TutorialMod.CONFIG.parkourEnabled ? "ON" : "OFF"));
        }
        parkourToggleWasPressed = isParkourTogglePressed;

        // --- Toggle Sprint Mode Hotkey ---
        boolean isSprintModePressed = isKeyDown(TutorialMod.CONFIG.sprintModeHotkey);
        if (isSprintModePressed && !sprintModeWasPressed) {
            var sprintToggled = ((GameOptionsAccessor) client.options).getSprintToggled();
            boolean newValue = !sprintToggled.getValue();
            sprintToggled.setValue(newValue);
            // Reset state when switching to Hold mode
            if (!newValue) {
                client.options.sprintKey.setPressed(isKeyCurrentlyPressed(client.options.sprintKey, client));
            }
            client.options.write();
            String mode = newValue ? "Toggle" : "Hold";
            TutorialMod.sendUpdateMessage("Sprint Mode set to " + mode);
        }
        sprintModeWasPressed = isSprintModePressed;

        // --- Toggle Sneak Mode Hotkey ---
        boolean isSneakModePressed = isKeyDown(TutorialMod.CONFIG.sneakModeHotkey);
        if (isSneakModePressed && !sneakModeWasPressed) {
            var sneakToggled = ((GameOptionsAccessor) client.options).getSneakToggled();
            boolean newValue = !sneakToggled.getValue();
            sneakToggled.setValue(newValue);
            // Reset state when switching to Hold mode
            if (!newValue) {
                client.options.sneakKey.setPressed(isKeyCurrentlyPressed(client.options.sneakKey, client));
            }
            client.options.write();
            String mode = newValue ? "Toggle" : "Hold";
            TutorialMod.sendUpdateMessage("Sneak Mode set to " + mode);
        }
        sneakModeWasPressed = isSneakModePressed;
    }

    private void handleTeammateKeybind(MinecraftClient client) {
        PlayerEntity target = getPlayerLookingAt(client, 9.0);
        if (target != null) {
            String name = target.getName().getString();
            if (TutorialMod.CONFIG.teamManager.isTeammate(name)) {
                TutorialMod.CONFIG.teamManager.removeTeammate(name);
                TutorialMod.sendUpdateMessage("Removed " + name + " from your team.");
            } else {
                if (TutorialMod.CONFIG.teamManager.addTeammate(name)) {
                    TutorialMod.sendUpdateMessage("Added " + name + " to your team.");
                }
            }
        }
    }


    private void handlePlacementSequence(MinecraftClient client) {
        if (actionTimeout > 0) {
            actionTimeout--;
            if (client.player != null && ((PlayerInventoryMixin) client.player.getInventory()).getSelectedSlot() != utilitySlot && utilitySlot != -1) {
                actionTimeout = 0;
            }
        }
        if (placementCooldown > 0) {
            placementCooldown--;
        }
        if (placementCooldown == 0) {
            PlacementAction action = nextPlacementAction;
            if (action == PlacementAction.NONE) {
                placementCooldown = -1;
                return;
            }
            nextPlacementAction = PlacementAction.NONE;
            if (client.player == null || client.interactionManager == null) return;
            PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
            switch (action) {
                case PLACE_TNT_MINECART:
                    int minecartSlot = findTntMinecartInHotbar(client.player);
                    if (minecartSlot != -1) {
                        if (railPos != null) {
                            // Survival Fix: Wait until rail is actually present in the client world
                            if (!(client.world.getBlockState(railPos).getBlock() instanceof AbstractRailBlock)) {
                                if (actionTimeout == -1) actionTimeout = 40; // Increased timeout for slower servers
                                if (actionTimeout > 0) {
                                    actionTimeout--;
                                    placementCooldown = 1;
                                    nextPlacementAction = PlacementAction.PLACE_TNT_MINECART;
                                    return;
                                }
                                railPos = null;
                                placementCooldown = -1;
                                actionTimeout = -1;
                                isMinecartSynced = false;
                                return;
                            }
                            actionTimeout = -1;

                            if (!isMinecartSynced) {
                                syncSlot(minecartSlot);
                                isMinecartSynced = true;
                                placementCooldown = 1; // Wait 1 tick for sync
                                nextPlacementAction = PlacementAction.PLACE_TNT_MINECART;
                                return;
                            }

                            isMinecartSynced = false;
                            ((MinecraftClientAccessor) client).setItemUseCooldown(0);

                            BlockHitResult bhr = new BlockHitResult(
                                new Vec3d(railPos.getX() + 0.5, railPos.getY() + 0.5, railPos.getZ() + 0.5),
                                Direction.UP, railPos, false
                            );

                            if (client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, bhr).isAccepted()) {
                                client.player.swingHand(Hand.MAIN_HAND);
                                awaitingMinecartConfirmationCooldown = 60;
                                placementCooldown = -1;
                            } else {
                                // Retry next tick if interaction failed for some reason (e.g. cooldown)
                                placementCooldown = 1;
                                nextPlacementAction = PlacementAction.PLACE_TNT_MINECART;
                            }
                        } else {
                            placementCooldown = -1;
                            isMinecartSynced = false;
                        }
                    }
                    break;
                case AWAITING_UTILITY_USE:
                    if (actionTimeout == 0) {
                        utilitySlot = -1;
                        crossbowSlot = -1;
                    } else {
                        placementCooldown = 1;
                        nextPlacementAction = action;
                    }
                    break;
                case SWITCH_TO_CROSSBOW:
                    if (crossbowSlot != -1) {
                        syncSlot(crossbowSlot);
                    }
                    utilitySlot = -1;
                    crossbowSlot = -1;
                    break;
                default: break;
            }
        }
    }

    private void handleConfirmationCooldowns(MinecraftClient client) {
        if (awaitingRailConfirmationCooldown > 0) {
            awaitingRailConfirmationCooldown--;
        }
        if (awaitingMinecartConfirmationCooldown > 0) {
            awaitingMinecartConfirmationCooldown--;
            if (awaitingMinecartConfirmationCooldown == 0) {
                railPos = null;
            }
        }
    }

    public static void setAwaitingRailConfirmation() {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.tntMinecartPlacementEnabled) return;
        awaitingRailConfirmationCooldown = 2;
    }

    public static void triggerImmediateRailPlacement(BlockPos pos) {
        if (instance != null) instance.startRailPlacement(pos);
    }

    public static void confirmRailPlacement(BlockPos pos, BlockState state) {
    }

    public static void confirmLavaPlacement(BlockPos pos, BlockState state) {
    }

    public static void confirmFirePlacement(BlockPos pos, BlockState state) {
    }

    public static void recordBowUsage() {
        if (instance != null) {
            instance.lastBowShotTick = MinecraftClient.getInstance().world.getTime();
        }
    }

    public void startRailPlacement(BlockPos pos) {
        if (placementCooldown != -1) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || findTntMinecartInHotbar(client.player) == -1) return;
        this.railPos = pos;
        this.placementCooldown = 1;
        this.nextPlacementAction = PlacementAction.PLACE_TNT_MINECART;
        this.actionTimeout = -1;
        this.isMinecartSynced = false;
    }

    public void startPostMinecartSequence(MinecraftClient client) {
        if (client.player == null) return;
        this.railPos = null;
        PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
        if (TutorialMod.CONFIG.lavaCrossbowSequenceEnabled) {
            int crossSlot = findLoadedCrossbowInHotbar(client.player);
            if (crossSlot != -1) {
                int lavaSlot = findLavaBucketInHotbar(client.player);
                if (lavaSlot != -1) {
                    this.utilitySlot = lavaSlot;
                    this.crossbowSlot = crossSlot;
                    syncSlot(lavaSlot);
                    this.actionTimeout = 200; // Increased timeout
                    this.placementCooldown = 1;
                    this.nextPlacementAction = PlacementAction.AWAITING_UTILITY_USE;
                    return;
                }
                int flintSlot = findFlintAndSteelInHotbar(client.player);
                if (flintSlot != -1) {
                    this.utilitySlot = flintSlot;
                    this.crossbowSlot = crossSlot;
                    syncSlot(flintSlot);
                    this.actionTimeout = 200;
                    this.placementCooldown = 1;
                    this.nextPlacementAction = PlacementAction.AWAITING_UTILITY_USE;
                    return;
                }
            }
        }
        if (TutorialMod.CONFIG.bowSequenceEnabled) {
            long currentTime = client.world.getTime();
            if (lastBowShotTick == -1 || (currentTime - lastBowShotTick) > TutorialMod.CONFIG.bowCooldown) {
                int bowSlot = findBowInHotbar(client.player);
                if (bowSlot != -1) {
                    inventory.setSelectedSlot(bowSlot);
                }
            }
        }
    }

    private boolean isArmored(PlayerEntity player) {
        if (!player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) return true;
        if (!player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) return true;
        if (!player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()) return true;
        if (!player.getEquippedStack(EquipmentSlot.FEET).isEmpty()) return true;
        return false;
    }

    private int findAxeInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    public boolean isMeleeWeapon(ItemStack stack) {
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.getItem() == Items.MACE || stack.isIn(ItemTags.SPEARS) || stack.isOf(Items.TRIDENT);
    }

    private void executeLungeSwap(PlayerEntity player, Entity target, int spearSlot) {
        if (isLungeSwapping || player == null || target == null) return;
        isLungeSwapping = true;
        MinecraftClient client = MinecraftClient.getInstance();

        int originalSlot = ((PlayerInventoryMixin) player.getInventory()).getSelectedSlot();

        try {
            syncSlot(spearSlot);
            if (client.interactionManager != null) {
                client.interactionManager.attackEntity(player, target);
                player.swingHand(Hand.MAIN_HAND);
            }

            int delay = TutorialMod.CONFIG.lungeSwapBackDelay;
            if (delay <= 0) {
                syncSlot(originalSlot);
            } else {
                this.comboRestoreSlot = originalSlot;
                this.comboRestoreTicks = delay;
            }
        } finally {
            isLungeSwapping = false;
        }
    }

    public boolean onLungeSwap() {
        if (!TutorialMod.CONFIG.masterEnabled || isExecutingCombo) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return false;

        boolean isHoldingMelee = isMeleeWeapon(client.player.getMainHandStack());
        boolean isMidAir = (!client.player.isOnGround() || client.player.fallDistance > 0) && !client.player.isGliding();

        if (client.crosshairTarget instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            if (client.player.getCameraPosVec(1.0f).distanceTo(bhr.getPos()) < 5.0) {
                return false;
            }
        }

        if (TutorialMod.CONFIG.lungeSwapEnabled && isMidAir && !isHoldingMelee) {
            int spearSlot = findSpearInHotbar(client.player, true);
            if (spearSlot != -1) {
                int originalSlot = ((PlayerInventoryMixin) client.player.getInventory()).getSelectedSlot();
                syncSlot(spearSlot);
                if (TutorialMod.CONFIG.lungeSwapBackDelay > 0) {
                    this.comboRestoreSlot = originalSlot;
                    this.comboRestoreTicks = TutorialMod.CONFIG.lungeSwapBackDelay;
                }
                return false; // Continue with attack using spear
            }
        }

        // Reach Swap logic (requires a target)
        return onReachSwap();
    }

    public boolean onReachSwap() {
        if (!TutorialMod.CONFIG.masterEnabled || isExecutingCombo) return false;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.interactionManager == null) return false;

        // Check if there is a target within spear reach (4.1)
        Entity target = null;
        if (client.crosshairTarget instanceof net.minecraft.util.hit.EntityHitResult ehr) {
            target = ehr.getEntity();
        }
        if (target == null) {
            target = getEntityLookingAt(client, TutorialMod.CONFIG.spearReachSwapRange, TutorialMod.CONFIG.reachSwapIgnoreCobwebs);
        }

        if (target != null) {
            double dist = client.player.distanceTo(target);
            boolean hasSpear = findSpearInHotbar(client.player, false) != -1;
            boolean needsReachSwap = TutorialMod.CONFIG.spearReachSwapEnabled && dist > TutorialMod.CONFIG.reachSwapActivationRange && hasSpear;

            if (needsReachSwap) {
                if (target instanceof PlayerEntity tp) {
                    executeCombatCombo(client.player, tp, false);
                    return true;
                } else {
                    int spearSlot = findSpearInHotbar(client.player, false);
                    if (spearSlot != -1) {
                        executeLungeSwap(client.player, target, spearSlot);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isKeyCurrentlyPressed(net.minecraft.client.option.KeyBinding keyBinding, MinecraftClient client) {
        return isKeyDown(keyBinding.getBoundKeyTranslationKey());
    }

    private int findMaceInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.MACE) return i;
        }
        return -1;
    }

    private void handleQuickCrossbow(MinecraftClient client) {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.quickCrossbowEnabled) return;
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        ItemStack mainHand = client.player.getMainHandStack();
        ItemStack offHand = client.player.getOffHandStack();
        boolean isCrossbow = mainHand.getItem() == Items.CROSSBOW || offHand.getItem() == Items.CROSSBOW;

        boolean isEmpty = false;
        if (mainHand.getItem() == Items.CROSSBOW && !CrossbowItem.isCharged(mainHand)) {
            isEmpty = true;
        } else if (offHand.getItem() == Items.CROSSBOW && !CrossbowItem.isCharged(offHand)) {
            isEmpty = true;
        }

        if (client.options.useKey.isPressed()) {
            if (isEmpty) {
                crossbowUseTicks++;
                crossbowWasUsing = true;
            } else {
                crossbowUseTicks = 0;
                crossbowWasUsing = false;
            }
        } else {
            if (crossbowWasUsing) {
                // Tapped (released before threshold)
                if (crossbowUseTicks > 0 && crossbowUseTicks <= TutorialMod.CONFIG.quickCrossbowReloadThreshold) {
                    triggerQuickCrossbow(client);
                }
                crossbowUseTicks = 0;
                crossbowWasUsing = false;
            }
        }
    }

    private void triggerQuickCrossbow(MinecraftClient client) {
        if (client.player == null || client.interactionManager == null) return;

        // Check offhand first
        ItemStack offHand = client.player.getOffHandStack();
        if (offHand.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(offHand)) {
            // Find first hotbar slot that isn't a crossbow to switch main hand
            int nonCrossbowSlot = findNonCrossbowHotbarSlot(client.player);
            if (nonCrossbowSlot != -1) {
                syncSlot(nonCrossbowSlot);
            }
            // Fire offhand
            client.interactionManager.interactItem(client.player, Hand.OFF_HAND);
            return;
        }

        // Check hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(stack)) {
                syncSlot(i);
                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                return;
            }
        }
    }

    private int findNonCrossbowHotbarSlot(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() != Items.CROSSBOW) return i;
        }
        return -1;
    }

    public int findSpearInHotbar(PlayerEntity player, boolean requireLunge) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.isIn(ItemTags.SPEARS) || stack.isOf(Items.TRIDENT)) {
                boolean hasLunge = hasLungeEnchantment(stack, player);
                if (requireLunge) {
                    if (hasLunge) return i;
                } else {
                    if (!hasLunge) return i;
                }
            }
        }
        return -1;
    }

    private boolean hasLungeEnchantment(ItemStack stack, PlayerEntity player) {
        if (stack.isEmpty()) return false;
        try {
            var registry = player.getEntityWorld().getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
            for (var entry : stack.getEnchantments().getEnchantments()) {
                if (entry.getKey().isPresent()) {
                    if (entry.getKey().get().getValue().equals(net.minecraft.util.Identifier.ofVanilla("lunge"))) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private int findTntMinecartInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.TNT_MINECART) return i;
        }
        return -1;
    }

    private int findLavaBucketInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.LAVA_BUCKET) return i;
        }
        return -1;
    }

    private int findLoadedCrossbowInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int findBowInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.BOW) return i;
        }
        return -1;
    }

    private int findFlintAndSteelInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getStack(i);
            if (s.isOf(Items.FLINT_AND_STEEL) || s.isOf(Items.FIRE_CHARGE)) return i;
        }
        return -1;
    }

    private Entity getEntityLookingAt(MinecraftClient client, double maxDistance, boolean ignoreCobwebs) {
        if (client.world == null || client.player == null) return null;

        Vec3d start = client.player.getCameraPosVec(1.0f);
        Vec3d direction = client.player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(maxDistance));

        Entity closestEntity = null;
        double minDistance = maxDistance;

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player || !entity.isAlive() || !entity.isAttackable()) continue;

            Box box = entity.getBoundingBox().expand(entity.getTargetingMargin());
            java.util.Optional<Vec3d> hit = box.raycast(start, end);

            if (hit.isPresent()) {
                double dist = start.distanceTo(hit.get());
                if (dist < minDistance) {
                    if (isLineOfSightBlocked(start, hit.get(), ignoreCobwebs)) continue;

                    minDistance = dist;
                    closestEntity = entity;
                }
            }
        }
        return closestEntity;
    }

    private boolean isLineOfSightBlocked(Vec3d start, Vec3d end, boolean ignoreCobwebs) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        Vec3d currentStart = start;
        for (int i = 0; i < 10; i++) {
            if (currentStart.distanceTo(end) < 0.1) break;

            BlockHitResult hit = client.world.raycast(new RaycastContext(
                    currentStart, end,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    client.player
            ));

            if (hit.getType() == HitResult.Type.MISS) return false;

            BlockPos pos = hit.getBlockPos();
            if (ignoreCobwebs && client.world.getBlockState(pos).isOf(Blocks.COBWEB)) {
                currentStart = hit.getPos().add(end.subtract(start).normalize().multiply(0.01));
            } else {
                return hit.getPos().distanceTo(start) < end.distanceTo(start) - 0.05;
            }
        }
        return false;
    }

    private PlayerEntity getPlayerLookingAt(MinecraftClient client, double maxDistance) {
        Entity entity = getEntityLookingAt(client, maxDistance, false);
        return (entity instanceof PlayerEntity) ? (PlayerEntity) entity : null;
    }

    private void handleAutoWaterDrain(MinecraftClient client) {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.waterDrainEnabled || !TutorialMod.CONFIG.autoWaterDrainMode) return;
        if (client.player == null || client.world == null || client.player.isCreative()) return;

        if (currentWebWaterState != WebWaterState.NONE || client.player.isSwimming() || client.currentScreen != null) return;

        if (nextPlacementAction != PlacementAction.NONE || awaitingMinecartConfirmationCooldown > 0) {
            return;
        }

        if (lastPlacedWaterTick != -1 && client.world.getTime() - lastPlacedWaterTick < TutorialMod.CONFIG.bucketDrainPlaceDelay) {
            return;
        }
        if (drainRestoreTicks != -1 || drainSwitchToTicks != -1 || currentFallbackDrainState != FallbackDrainState.NONE) return;

        boolean isNether = client.world.getRegistryKey() == World.NETHER;

        double range = client.player.getBlockInteractionRange();
        Vec3d start = client.player.getCameraPosVec(1.0f);
        Vec3d dir = client.player.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(range));

        // Dual Raycast System
        BlockHitResult hitFluid = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.SOURCE_ONLY, client.player));
        BlockHitResult hitNone = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));

        // 1. Normal Drain (Empty Bucket)
        if (hitFluid.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = hitFluid.getBlockPos();
            var fluidState = client.world.getFluidState(pos);
            if (fluidState.isStill()) {
                boolean isWater = fluidState.isIn(net.minecraft.registry.tag.FluidTags.WATER);
                boolean isLava = fluidState.isIn(net.minecraft.registry.tag.FluidTags.LAVA);

                boolean canDrainNormally = true;
                if (isWater && (isNether || isSurroundedByWaterSource(client.world, pos))) canDrainNormally = false;
                if (isLava) {
                    if (!TutorialMod.CONFIG.waterDrainLavaEnabled) canDrainNormally = false;
                    else {
                        boolean isSubmerged = client.player.isSubmergedIn(net.minecraft.registry.tag.FluidTags.LAVA);
                        if (!isSubmerged && client.player.getPitch() < TutorialMod.CONFIG.lavaDrainMinPitch) canDrainNormally = false;
                    }
                }

                if (canDrainNormally) {
                    if (isLava && !client.player.isInLava()) return;
                    int bucketSlot = findEmptyBucketInHotbar(client.player);
                    if (bucketSlot != -1) {
                        PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
                        originalSlotBeforeDrain = inventory.getSelectedSlot();
                        if (inventory.getSelectedSlot() != bucketSlot) {
                            if (TutorialMod.CONFIG.waterDrainSwitchToDelay > 0) {
                                drainPendingSlot = bucketSlot;
                                drainSwitchToTicks = TutorialMod.CONFIG.waterDrainSwitchToDelay;
                            } else {
                                syncSlot(bucketSlot);
                                if (client.interactionManager != null) {
                                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                                    client.player.swingHand(Hand.MAIN_HAND);
                                }
                                drainRestoreTicks = 2 + TutorialMod.CONFIG.waterDrainSwitchBackDelay;
                                drainSwitchBackTimer = -1;
                            }
                        } else {
                            if (client.interactionManager != null) {
                                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                                client.player.swingHand(Hand.MAIN_HAND);
                            }
                            drainRestoreTicks = 2;
                            drainSwitchBackTimer = -1;
                        }
                        return;
                    }
                }
            }
        }

        // 2. Fallback Drain (Displacement)
        if (hitNone.getType() == HitResult.Type.BLOCK) {
            BlockPos liquidPos = hitNone.getBlockPos().offset(hitNone.getSide());
            var fluidState = client.world.getFluidState(liquidPos);
            if (fluidState.isStill()) {
                boolean isWater = fluidState.isIn(net.minecraft.registry.tag.FluidTags.WATER);
                boolean isLava = fluidState.isIn(net.minecraft.registry.tag.FluidTags.LAVA);
                if ((isWater && !isNether && !isSurroundedByWaterSource(client.world, liquidPos)) || (isLava && TutorialMod.CONFIG.waterDrainLavaEnabled)) {
                    handleFallbackDrainThrough(client, hitNone, isWater, isLava);
                }
            }
        }
    }

    private boolean isSurroundedByWaterSource(World world, BlockPos pos) {
        int sources = 0;
        Direction[] horizontal = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
        for (Direction d : horizontal) {
            if (world.getFluidState(pos.offset(d)).isStill()) sources++;
        }
        return sources >= 1;
    }

    private void handleAutoExtinguish(MinecraftClient client) {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.autoExtinguishEnabled || client.currentScreen != null) {
            currentExtinguishState = ExtinguishState.NONE;
            return;
        }
        if (client.player == null || client.world == null || client.player.isCreative()) return;

        boolean isNether = client.world.getRegistryKey() == World.NETHER;

        if (currentExtinguishState == ExtinguishState.NONE) {
            if (client.player.isOnFire() && client.player.getFireTicks() > TutorialMod.CONFIG.autoExtinguishFireTicksThreshold && client.player.getPitch() >= TutorialMod.CONFIG.autoExtinguishPitch) {
                boolean lookingAtFire = client.crosshairTarget instanceof BlockHitResult bhr && client.world.getBlockState(bhr.getBlockPos()).isOf(Blocks.FIRE);

                if (lookingAtFire) {
                    PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
                    originalSlotBeforeExtinguish = inventory.getSelectedSlot();
                    currentExtinguishState = ExtinguishState.PUNCHING;
                    extinguishTimer = 0;
                } else if (!isNether && TutorialMod.CONFIG.waterDrainEnabled && findWaterBucketInHotbar(client.player) != -1) {
                    // Only use water if looking at a non-lava/non-water block or air
                    if (client.crosshairTarget instanceof BlockHitResult bhr) {
                        BlockState state = client.world.getBlockState(bhr.getBlockPos());
                        if (state.getFluidState().isEmpty() && !state.isOf(Blocks.LAVA) && !state.isOf(Blocks.WATER)) {
                            PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
                            originalSlotBeforeExtinguish = inventory.getSelectedSlot();
                            currentExtinguishState = ExtinguishState.PUNCHING; // Still go through PUNCHING first
                            extinguishTimer = 0;
                        }
                    }
                }
            }
        } else {
            if (extinguishTimer > 0) {
                extinguishTimer--;
                return;
            }

            switch (currentExtinguishState) {
                case PUNCHING:
                    if (client.crosshairTarget instanceof BlockHitResult bhr && client.world.getBlockState(bhr.getBlockPos()).isOf(Blocks.FIRE)) {
                        if (client.interactionManager != null) {
                            client.interactionManager.attackBlock(bhr.getBlockPos(), bhr.getSide());
                            client.player.swingHand(Hand.MAIN_HAND);
                        }
                    }

                    if (isNether) {
                        currentExtinguishState = ExtinguishState.NONE;
                        originalSlotBeforeExtinguish = -1;
                    } else {
                        int waterSlot = findWaterBucketInHotbar(client.player);
                        if (waterSlot != -1) {
                            if (TutorialMod.CONFIG.waterDrainSwitchToDelay > 0) {
                                currentExtinguishState = ExtinguishState.SWITCH_TO_BUCKET;
                                extinguishTimer = TutorialMod.CONFIG.waterDrainSwitchToDelay;
                            } else {
                                syncSlot(waterSlot);
                                currentExtinguishState = ExtinguishState.PLACING;
                                extinguishTimer = 0;
                            }
                        } else {
                            currentExtinguishState = ExtinguishState.NONE;
                            originalSlotBeforeExtinguish = -1;
                        }
                    }
                    break;
                case SWITCH_TO_BUCKET:
                    int waterSlot = findWaterBucketInHotbar(client.player);
                    if (waterSlot != -1) {
                        syncSlot(waterSlot);
                        currentExtinguishState = ExtinguishState.PLACING;
                        extinguishTimer = 0;
                    } else {
                        currentExtinguishState = ExtinguishState.NONE;
                        originalSlotBeforeExtinguish = -1;
                    }
                    break;
                case PLACING:
                    if (client.interactionManager != null) {
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                        client.player.swingHand(Hand.MAIN_HAND);
                        currentExtinguishState = ExtinguishState.PICKING_UP;
                        extinguishTimer = 1;
                    } else {
                        currentExtinguishState = ExtinguishState.NONE;
                        originalSlotBeforeExtinguish = -1;
                    }
                    break;
                case PICKING_UP:
                    if (client.interactionManager != null) {
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                        client.player.swingHand(Hand.MAIN_HAND);
                        currentExtinguishState = ExtinguishState.SWITCHING_BACK;
                        extinguishTimer = TutorialMod.CONFIG.waterDrainSwitchBackDelay;
                    } else {
                        currentExtinguishState = ExtinguishState.NONE;
                        originalSlotBeforeExtinguish = -1;
                    }
                    break;
                case SWITCHING_BACK:
                    if (originalSlotBeforeExtinguish != -1) {
                        syncSlot(originalSlotBeforeExtinguish);
                    }
                    currentExtinguishState = ExtinguishState.NONE;
                    originalSlotBeforeExtinguish = -1;
                    break;
                default:
                    currentExtinguishState = ExtinguishState.NONE;
                    break;
            }
        }
    }

    private int findWaterBucketInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.WATER_BUCKET) return i;
        }
        return -1;
    }

    public void onPostItemUse(PlayerEntity player, Hand hand) {
        if (!TutorialMod.CONFIG.masterEnabled || hand != Hand.MAIN_HAND) return;

        // --- Minecart Sequence manual use check ---
        if (nextPlacementAction == PlacementAction.AWAITING_UTILITY_USE) {
            int currentSlot = ((PlayerInventoryMixin) player.getInventory()).getSelectedSlot();
            if (currentSlot == utilitySlot) {
                nextPlacementAction = PlacementAction.SWITCH_TO_CROSSBOW;
                placementCooldown = 2; // Slight delay for server sync
                return;
            }
        }

        ItemStack stack = player.getStackInHand(hand);
        if (TutorialMod.CONFIG.iceGhostSwapEnabled && isIceBlock(stack)) {
            int airSlot = findAirSlot(player);
            if (airSlot != -1) {
                int originalSlot = ((PlayerInventoryMixin) player.getInventory()).getSelectedSlot();
                syncSlot(airSlot);
                this.iceGhostRestoreSlot = originalSlot;
                this.iceGhostSwapTicks = TutorialMod.CONFIG.iceGhostSwapDelay;
            }
        }
    }

    public boolean onItemUse() {
        if (!TutorialMod.CONFIG.masterEnabled) return false;
        if (ignoreNextUse) return true;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) return false;

        if (client.player.isCreative()) return false;

        ItemStack stack = client.player.getMainHandStack();

        if (stack.getItem() instanceof net.minecraft.item.BlockItem) {
            lastBlockPlaceTick = client.world.getTime();
        }
        ItemStack offhandStack = client.player.getOffHandStack();

        if (TutorialMod.CONFIG.autoElytraFlyEnabled && (stack.isOf(Items.ELYTRA) || offhandStack.isOf(Items.ELYTRA))) {
            elytraFlyTimer = 10;
        }

        if (stack.isOf(Items.WATER_BUCKET)) {
            lastPlacedWaterTick = client.world.getTime();
        }

        // Manual Drain Check
        if (currentWebWaterState != WebWaterState.NONE || !TutorialMod.CONFIG.waterDrainEnabled || TutorialMod.CONFIG.autoWaterDrainMode || client.currentScreen != null || client.player.isSwimming()) return false;

        boolean isNether = client.world.getRegistryKey() == World.NETHER;

        double range = client.player.getBlockInteractionRange();
        Vec3d start = client.player.getCameraPosVec(1.0f);
        Vec3d dir = client.player.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(range));
        BlockHitResult hitFluid = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.SOURCE_ONLY, client.player));
        BlockHitResult hitNone = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));

        if (hitFluid.getType() == HitResult.Type.BLOCK) {
            var fluidState = client.world.getFluidState(hitFluid.getBlockPos());
            if (fluidState.isStill()) {
                boolean isWater = fluidState.isIn(net.minecraft.registry.tag.FluidTags.WATER);
                boolean isLava = fluidState.isIn(net.minecraft.registry.tag.FluidTags.LAVA);

                if (isWater) {
                    if (isNether || isSurroundedByWaterSource(client.world, hitFluid.getBlockPos())) return false;
                    if (lastPlacedWaterTick != -1 && client.world.getTime() - lastPlacedWaterTick < TutorialMod.CONFIG.bucketDrainPlaceDelay) return false;
                }
                if (isLava) {
                    if (!TutorialMod.CONFIG.waterDrainLavaEnabled || !client.player.isInLava()) return false;
                }

                int bucketSlot = findEmptyBucketInHotbar(client.player);
                if (bucketSlot != -1) {
                    PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
                    originalSlotBeforeDrain = inventory.getSelectedSlot();
                    if (inventory.getSelectedSlot() != bucketSlot) {
                        if (TutorialMod.CONFIG.waterDrainSwitchToDelay > 0) {
                            drainPendingSlot = bucketSlot;
                            drainSwitchToTicks = TutorialMod.CONFIG.waterDrainSwitchToDelay;
                        } else {
                            syncSlot(bucketSlot);
                            if (client.interactionManager != null) {
                                client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                                client.player.swingHand(Hand.MAIN_HAND);
                            }
                            drainRestoreTicks = 2 + TutorialMod.CONFIG.waterDrainSwitchBackDelay;
                            drainSwitchBackTimer = -1;
                        }
                    } else {
                        if (client.interactionManager != null) {
                            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                            client.player.swingHand(Hand.MAIN_HAND);
                        }
                        drainRestoreTicks = 2;
                        drainSwitchBackTimer = -1;
                    }
                    return true;
                }
            }
        }

        if (hitNone.getType() == HitResult.Type.BLOCK) {
            BlockPos liquidPos = hitNone.getBlockPos().offset(hitNone.getSide());
            var fluidState = client.world.getFluidState(liquidPos);
            if (fluidState.isStill()) {
                boolean isWater = fluidState.isIn(net.minecraft.registry.tag.FluidTags.WATER);
                boolean isLava = fluidState.isIn(net.minecraft.registry.tag.FluidTags.LAVA);
                if ((isWater && !isNether && !isSurroundedByWaterSource(client.world, liquidPos)) || (isLava && TutorialMod.CONFIG.waterDrainLavaEnabled)) {
                    if (findEmptyBucketInHotbar(client.player) == -1) {
                        handleFallbackDrainThrough(client, hitNone, isWater, isLava);
                        if (currentFallbackDrainState != FallbackDrainState.NONE) return true;
                    }
                }
            }
        }
        return false;
    }

    private void handleWaterDrainSwitchTo(MinecraftClient client) {
        if (drainSwitchToTicks > 0) {
            drainSwitchToTicks--;
            if (drainSwitchToTicks == 0) {
                if (drainPendingSlot != -1) {
                    syncSlot(drainPendingSlot);
                    if (client.interactionManager != null && client.player != null) {
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                    drainRestoreTicks = 20 + TutorialMod.CONFIG.waterDrainSwitchBackDelay;
                    drainSwitchBackTimer = -1;
                    drainPendingSlot = -1;
                }
                drainSwitchToTicks = -1;
            }
        }
    }

    private void handleWaterDrainRestore(MinecraftClient client) {
        if (drainRestoreTicks > 0) {
            drainRestoreTicks--;

            ItemStack held = client.player.getMainHandStack();
            boolean isFull = held.getItem() == Items.WATER_BUCKET || held.getItem() == Items.LAVA_BUCKET;

            if (isFull && drainSwitchBackTimer == -1) {
                drainSwitchBackTimer = TutorialMod.CONFIG.waterDrainSwitchBackDelay;
                if (drainSwitchBackTimer == 0) {
                    restoreDrainSlot();
                    drainRestoreTicks = -1;
                    drainSwitchBackTimer = -1;
                    return;
                }
            }

            if (drainSwitchBackTimer > 0) {
                drainSwitchBackTimer--;
                if (drainSwitchBackTimer == 0) {
                    restoreDrainSlot();
                    drainRestoreTicks = -1;
                    drainSwitchBackTimer = -1;
                    return;
                }
            }
        } else if (drainRestoreTicks == 0) {
            restoreDrainSlot();
            drainRestoreTicks = -1;
            drainSwitchBackTimer = -1;
        }
    }

    private void restoreDrainSlot() {
        if (originalSlotBeforeDrain != -1) {
            int delay = TutorialMod.CONFIG.bucketDrainRestoreDelay;
            if (delay <= 0) {
                syncSlot(originalSlotBeforeDrain);
            } else {
                this.comboRestoreSlot = originalSlotBeforeDrain;
                this.comboRestoreTicks = delay;
            }
            originalSlotBeforeDrain = -1;
        }
    }

    private void handleSelfWaterWeb(MinecraftClient client) {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.selfWaterWebEnabled || client.currentScreen != null) {
            currentWebWaterState = WebWaterState.NONE;
            return;
        }
        if (client.player == null || client.world == null) return;

        if (selfWaterWebCooldown > 0) {
            selfWaterWebCooldown--;
        }

        if (currentWebWaterState == WebWaterState.NONE) {
            if (selfWaterWebCooldown > 0) return;
            boolean isNether = client.world.getRegistryKey() == World.NETHER;
            if (!client.player.isSneaking() && !isNether && client.player.getPitch() > 85.0f && (client.world.getBlockState(client.player.getBlockPos()).isOf(Blocks.COBWEB) || client.world.getBlockState(client.player.getBlockPos().up()).isOf(Blocks.COBWEB))) {
                int waterSlot = findWaterBucketInHotbar(client.player);
                if (waterSlot != -1) {
                    originalSlotBeforeWebWater = ((PlayerInventoryMixin)client.player.getInventory()).getSelectedSlot();
                    if (TutorialMod.CONFIG.selfWaterWebSwitchDelay > 0) {
                        currentWebWaterState = WebWaterState.SWITCH_TO_BUCKET;
                        webWaterStateTimer = TutorialMod.CONFIG.selfWaterWebSwitchDelay;
                    } else {
                        syncSlot(waterSlot);
                        currentWebWaterState = WebWaterState.PLACING;
                        webWaterStateTimer = Math.max(2, TutorialMod.CONFIG.selfWaterWebPlaceDelay);
                    }
                }
            }
        } else {
            if (webWaterStateTimer > 0) {
                webWaterStateTimer--;
                return;
            }

            switch (currentWebWaterState) {
                case SWITCH_TO_BUCKET:
                    int waterSlot = findWaterBucketInHotbar(client.player);
                    if (waterSlot != -1) {
                        syncSlot(waterSlot);
                        currentWebWaterState = WebWaterState.PLACING;
                        webWaterStateTimer = Math.max(2, TutorialMod.CONFIG.selfWaterWebPlaceDelay);
                    } else {
                        currentWebWaterState = WebWaterState.NONE;
                    }
                    break;
                case PLACING:
                    if (client.interactionManager != null) {
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                        client.player.swingHand(Hand.MAIN_HAND);
                        currentWebWaterState = WebWaterState.PICKING_UP;
                        webWaterStateTimer = Math.max(10, TutorialMod.CONFIG.selfWaterWebPickDelay);
                    } else {
                        currentWebWaterState = WebWaterState.NONE;
                    }
                    break;
                case PICKING_UP:
                    if (client.interactionManager != null) {
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                        client.player.swingHand(Hand.MAIN_HAND);
                        currentWebWaterState = WebWaterState.RESTORING;
                        webWaterStateTimer = TutorialMod.CONFIG.selfWaterWebRestoreDelay;
                    } else {
                        currentWebWaterState = WebWaterState.NONE;
                    }
                    break;
                case RESTORING:
                    if (originalSlotBeforeWebWater != -1) {
                        syncSlot(originalSlotBeforeWebWater);
                    }
                    currentWebWaterState = WebWaterState.NONE;
                    originalSlotBeforeWebWater = -1;
                    selfWaterWebCooldown = 20;
                    break;
                default:
                    currentWebWaterState = WebWaterState.NONE;
                    break;
            }
        }
    }

    private void handleCounterLavaDrain(MinecraftClient client) {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.counterLavaDrainEnabled || client.currentScreen != null) {
            currentCounterLavaState = CounterLavaState.NONE;
            return;
        }
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        if (currentCounterLavaState == CounterLavaState.NONE) {
            double range = client.player.getBlockInteractionRange();
            Vec3d start = client.player.getCameraPosVec(1.0f);
            Vec3d dir = client.player.getRotationVec(1.0f);
            Vec3d end = start.add(dir.multiply(range));
            BlockHitResult hit = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.SOURCE_ONLY, client.player));

            if (hit.getType() == HitResult.Type.BLOCK && client.world.getFluidState(hit.getBlockPos()).isIn(net.minecraft.registry.tag.FluidTags.LAVA)) {
                PlayerEntity enemy = getPlayerLookingAt(client, TutorialMod.CONFIG.counterLavaDrainRange);
                if (enemy != null) {
                    ItemStack main = enemy.getMainHandStack();
                    ItemStack off = enemy.getOffHandStack();
                    if (isCounterItem(main) || isCounterItem(off)) {
                        int bucketSlot = findEmptyBucketInHotbar(client.player);
                        if (bucketSlot != -1) {
                            originalSlotBeforeCounterLava = ((PlayerInventoryMixin)client.player.getInventory()).getSelectedSlot();
                            currentCounterLavaState = CounterLavaState.SWITCH_TO_EMPTY;
                            counterLavaTimer = TutorialMod.CONFIG.counterLavaDrainSwitchDelay;
                        }
                    }
                }
            }
        } else {
            if (counterLavaTimer > 0) {
                counterLavaTimer--;
                return;
            }

            switch (currentCounterLavaState) {
                case SWITCH_TO_EMPTY:
                    int bucketSlot = findEmptyBucketInHotbar(client.player);
                    if (bucketSlot != -1) {
                        syncSlot(bucketSlot);
                        currentCounterLavaState = CounterLavaState.PICKING_UP;
                        counterLavaTimer = TutorialMod.CONFIG.counterLavaDrainPickDelay;
                    } else {
                        currentCounterLavaState = CounterLavaState.NONE;
                    }
                    break;
                case PICKING_UP:
                    if (client.interactionManager != null) {
                        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                        client.player.swingHand(Hand.MAIN_HAND);
                        currentCounterLavaState = CounterLavaState.RESTORING;
                        counterLavaTimer = TutorialMod.CONFIG.counterLavaDrainRestoreDelay;
                    } else {
                        currentCounterLavaState = CounterLavaState.NONE;
                    }
                    break;
                case RESTORING:
                    if (originalSlotBeforeCounterLava != -1) {
                        syncSlot(originalSlotBeforeCounterLava);
                    }
                    currentCounterLavaState = CounterLavaState.NONE;
                    originalSlotBeforeCounterLava = -1;
                    break;
                default:
                    currentCounterLavaState = CounterLavaState.NONE;
                    break;
            }
        }
    }

    private void handleAntiLavaFlow(MinecraftClient client) {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.antiLavaFlowEnabled || client.currentScreen != null) {
            currentAntiLavaFlowState = AntiLavaFlowState.NONE;
            return;
        }
        if (client.player == null || client.world == null) return;

        if (currentAntiLavaFlowState == AntiLavaFlowState.NONE && !client.player.getMainHandStack().isOf(Items.BUCKET)) {
            return;
        }

        if (client.options.attackKey.isPressed()) {
            if (currentAntiLavaFlowState != AntiLavaFlowState.NONE) {
                if (currentAntiLavaFlowState == AntiLavaFlowState.PLACING || currentAntiLavaFlowState == AntiLavaFlowState.RESTORING) {
                    int bucketSlot = findEmptyBucketInHotbar(client.player);
                    if (bucketSlot != -1) {
                        syncSlot(bucketSlot);
                        if (client.interactionManager != null) {
                            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                            client.player.swingHand(Hand.MAIN_HAND);
                        }
                    }
                }
                currentAntiLavaFlowState = AntiLavaFlowState.NONE;
                originalSlotBeforeAntiLava = -1;
                return;
            }
        }

        if (currentAntiLavaFlowState == AntiLavaFlowState.NONE) {
            double range = client.player.getBlockInteractionRange();
            Vec3d start = client.player.getCameraPosVec(1.0f);
            Vec3d dir = client.player.getRotationVec(1.0f);
            Vec3d end = start.add(dir.multiply(range));
            BlockHitResult hit = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.SOURCE_ONLY, client.player));

            if (hit.getType() == HitResult.Type.BLOCK && client.world.getFluidState(hit.getBlockPos()).isIn(net.minecraft.registry.tag.FluidTags.LAVA)) {
                boolean enemyNear = false;
                double enemyRangeSq = TutorialMod.CONFIG.antiLavaFlowEnemyRange * TutorialMod.CONFIG.antiLavaFlowEnemyRange;
                Vec3d lavaPos = hit.getPos();
                for (PlayerEntity p : client.world.getPlayers()) {
                    if (p == client.player || TutorialMod.CONFIG.teamManager.isTeammate(p.getName().getString())) continue;
                    if (p.squaredDistanceTo(lavaPos) <= enemyRangeSq) {
                        enemyNear = true;
                        break;
                    }
                }

                if (enemyNear) {
                    int bucketSlot = findEmptyBucketInHotbar(client.player);
                    if (bucketSlot != -1) {
                        originalSlotBeforeAntiLava = ((PlayerInventoryMixin)client.player.getInventory()).getSelectedSlot();
                        currentAntiLavaFlowState = AntiLavaFlowState.SWITCH_TO_EMPTY;
                        antiLavaFlowTimer = TutorialMod.CONFIG.antiLavaFlowPickDelay;
                    }
                }
            }
        } else {
            if (antiLavaFlowTimer > 0) {
                antiLavaFlowTimer--;
                return;
            }

            switch (currentAntiLavaFlowState) {
                case SWITCH_TO_EMPTY:
                    int bucketSlot = findEmptyBucketInHotbar(client.player);
                    if (bucketSlot != -1) {
                        syncSlot(bucketSlot);
                        if (client.interactionManager != null) {
                            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                            client.player.swingHand(Hand.MAIN_HAND);
                        }
                        currentAntiLavaFlowState = AntiLavaFlowState.HOLDING;
                        antiLavaFlowTimer = TutorialMod.CONFIG.antiLavaFlowHoldDelay;
                    } else {
                        currentAntiLavaFlowState = AntiLavaFlowState.NONE;
                    }
                    break;
                case HOLDING:
                    if (client.interactionManager != null) {
                        double range = client.player.getBlockInteractionRange();
                        Vec3d start = client.player.getCameraPosVec(1.0f);
                        Vec3d dir = client.player.getRotationVec(1.0f);
                        Vec3d end = start.add(dir.multiply(range));
                        BlockHitResult hit = client.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, client.player));

                        if (hit.getType() == HitResult.Type.BLOCK) {
                            BlockPos placePos = hit.getBlockPos().offset(hit.getSide());
                            Box playerBox = client.player.getBoundingBox();
                            if (playerBox.intersects(new Box(placePos))) {
                                currentAntiLavaFlowState = AntiLavaFlowState.NONE;
                                return;
                            }

                            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                            client.player.swingHand(Hand.MAIN_HAND);
                            currentAntiLavaFlowState = AntiLavaFlowState.RESTORING;
                            antiLavaFlowTimer = TutorialMod.CONFIG.antiLavaFlowRestoreDelay;
                        } else {
                            currentAntiLavaFlowState = AntiLavaFlowState.NONE;
                        }
                    } else {
                        currentAntiLavaFlowState = AntiLavaFlowState.NONE;
                    }
                    break;
                case RESTORING:
                    if (originalSlotBeforeAntiLava != -1) {
                        syncSlot(originalSlotBeforeAntiLava);
                    }
                    currentAntiLavaFlowState = AntiLavaFlowState.NONE;
                    originalSlotBeforeAntiLava = -1;
                    break;
                default:
                    currentAntiLavaFlowState = AntiLavaFlowState.NONE;
                    break;
            }
        }
    }

    private boolean isCounterItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.isOf(Items.BUCKET) || stack.isOf(Items.WATER_BUCKET) || stack.isOf(Items.COBWEB) || stack.isOf(Items.POWDER_SNOW_BUCKET)) return true;
        if (stack.getItem() instanceof net.minecraft.item.BlockItem bi) {
            return bi.getBlock() == Blocks.COBWEB || bi.getBlock() == Blocks.POWDER_SNOW;
        }
        return false;
    }

    private boolean isIceBlock(ItemStack stack) {
        return stack.isOf(Items.ICE) || stack.isOf(Items.PACKED_ICE) || stack.isOf(Items.BLUE_ICE);
    }

    private int findAirSlot(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    private int findEmptyBucketInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.BUCKET) return i;
        }
        return -1;
    }

    private int findSolidBlockInHotbar(PlayerEntity player, BlockPos targetPos) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof net.minecraft.item.BlockItem bi) {
                if (bi.getBlock().getDefaultState().isFullCube(player.getEntityWorld(), targetPos)) {
                    if (!player.getEntityWorld().canPlace(bi.getBlock().getDefaultState(), targetPos, net.minecraft.block.ShapeContext.of(player))) {
                        continue;
                    }
                    return i;
                }
            }
        }
        return -1;
    }

    private void handleFallbackDrainThrough(MinecraftClient client, BlockHitResult hitNone, boolean isWater, boolean isLava) {
        if (!TutorialMod.CONFIG.bucketDrainFallbackEnabled && !TutorialMod.CONFIG.blockDrainFallbackEnabled) return;
        BlockPos liquidPos = hitNone.getBlockPos().offset(hitNone.getSide());
        Direction targetSide = hitNone.getSide();
        BlockPos surfacePos = hitNone.getBlockPos();

        initiateFallback(client, liquidPos, surfacePos, targetSide, isWater, isLava);
    }

    private void initiateFallback(MinecraftClient client, BlockPos liquidPos, BlockPos surfacePos, Direction targetSide, boolean isWater, boolean isLava) {
        int slot = -1;
        boolean blockMode = false;

        if (TutorialMod.CONFIG.bucketDrainFallbackEnabled) {
            slot = findWaterBucketInHotbar(client.player);
            if (slot == -1 && isLava) {
                boolean hasFlowingWater = false;
                for (Direction d : Direction.values()) {
                    if (client.world.getFluidState(liquidPos.offset(d)).isIn(net.minecraft.registry.tag.FluidTags.WATER)) {
                        hasFlowingWater = true;
                        break;
                    }
                }
                if (!hasFlowingWater) {
                    slot = findLavaBucketInHotbar(client.player);
                }
            }
        }

        if (slot == -1 && TutorialMod.CONFIG.blockDrainFallbackEnabled) {
            slot = findSolidBlockInHotbar(client.player, liquidPos);
            if (slot != -1) blockMode = true;
        }

        if (slot != -1) {
            originalSlotBeforeFallback = ((PlayerInventoryMixin) client.player.getInventory()).getSelectedSlot();
            fallbackDrainTargetPos = surfacePos;
            fallbackDrainTargetSide = targetSide;
            fallbackDrainPendingSlot = slot;
            isBlockFallback = blockMode;
            currentFallbackDrainState = FallbackDrainState.START;
            fallbackDrainTimer = TutorialMod.CONFIG.waterDrainSwitchToDelay;
        }
    }

    private void handleFallbackDrainTick(MinecraftClient client) {
        if (currentFallbackDrainState == FallbackDrainState.NONE) return;

        if (fallbackDrainTimer > 0) {
            fallbackDrainTimer--;
            return;
        }

        switch (currentFallbackDrainState) {
            case START:
                syncSlot(fallbackDrainPendingSlot);
                currentFallbackDrainState = FallbackDrainState.PLACING;
                fallbackDrainTimer = 3;
                break;
            case PLACING:
                if (client.interactionManager != null && client.player != null) {
                    BlockHitResult bhr = new BlockHitResult(
                            new Vec3d(fallbackDrainTargetPos.getX() + 0.5, fallbackDrainTargetPos.getY() + 0.5, fallbackDrainTargetPos.getZ() + 0.5),
                            fallbackDrainTargetSide, fallbackDrainTargetPos, false);
                    client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, bhr);
                    client.player.swingHand(Hand.MAIN_HAND);
                    if (isBlockFallback) {
                        currentFallbackDrainState = FallbackDrainState.RESTORING;
                        fallbackDrainTimer = 3 + TutorialMod.CONFIG.waterDrainSwitchBackDelay;
                    } else {
                        currentFallbackDrainState = FallbackDrainState.PICKING_UP;
                        fallbackDrainTimer = 3;
                    }
                } else {
                    currentFallbackDrainState = FallbackDrainState.NONE;
                }
                break;
            case PICKING_UP:
                if (client.interactionManager != null && client.player != null) {
                    client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
                    client.player.swingHand(Hand.MAIN_HAND);
                    currentFallbackDrainState = FallbackDrainState.RESTORING;
                    fallbackDrainTimer = 3 + TutorialMod.CONFIG.waterDrainSwitchBackDelay;
                } else {
                    currentFallbackDrainState = FallbackDrainState.NONE;
                }
                break;
            case RESTORING:
                if (originalSlotBeforeFallback != -1) {
                    syncSlot(originalSlotBeforeFallback);
                }
                currentFallbackDrainState = FallbackDrainState.NONE;
                originalSlotBeforeFallback = -1;
                fallbackDrainTargetPos = null;
                fallbackDrainTargetSide = null;
                fallbackDrainPendingSlot = -1;
                break;
            default:
                currentFallbackDrainState = FallbackDrainState.NONE;
                break;
        }
    }

    private void handleChatMacros(MinecraftClient client) {
        if (client.player == null) return;

        try {
            if (InputUtil.isKeyPressed(client.getWindow(), GLFW.GLFW_KEY_F3)) {
                return;
            }
        } catch (Exception ignored) {}

        List<ModConfig.Macro> macros = new ArrayList<>(Arrays.asList(TutorialMod.CONFIG.macro1, TutorialMod.CONFIG.macro2, TutorialMod.CONFIG.macro3, TutorialMod.CONFIG.macro4, TutorialMod.CONFIG.macro5));

        for (ModConfig.Macro macro : macros) {
            if (macro.hotkey == null || macro.hotkey.equals("key.keyboard.unknown") || macro.message == null || macro.message.isEmpty()) {
                continue;
            }

            boolean isPressed = isKeyDown(macro.hotkey);
            boolean wasPressed = wasMacroKeyPressed.getOrDefault(macro.hotkey, false);

            if (isPressed && !wasPressed) {
                if (macro.message.startsWith("/")) {
                    client.player.networkHandler.sendChatCommand(macro.message.substring(1));
                } else {
                    client.player.networkHandler.sendChatMessage(macro.message);
                }
            }
            wasMacroKeyPressed.put(macro.hotkey, isPressed);
        }
    }

    private static String formatCoords(ModConfig cfg) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return cfg.trigger; // fallback

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        long bx = (long) Math.floor(x);
        long by = (long) Math.floor(y);
        long bz = (long) Math.floor(z);

        String sx = String.format("%.2f", x);
        String sy = String.format("%.2f", y);
        String sz = String.format("%.2f", z);

        String dim = "";
        try {
            RegistryKey<World> key = client.world.getRegistryKey();
            Identifier id = key.getValue();
            if (id != null) {
                String dimensionName = id.getPath().replace("the_", "");
                dim = " " + dimensionName;
            }
        } catch (Exception ignored) {
        }

        String facing = "";
        try {
            Direction d = player.getHorizontalFacing();
            if (d != null) facing = " " + d.toString().toLowerCase();
        } catch (Exception ignored) {
        }

        String out = cfg.format;
        out = out.replace("{x}", sx).replace("{y}", sy).replace("{z}", sz);
        out = out.replace("{bx}", Long.toString(bx)).replace("{by}", Long.toString(by)).replace("{bz}", Long.toString(bz));
        out = out.replace("{dim}", dim);
        out = out.replace("{facing}", facing);
        return out;
    }

    public String formatCoordsForOverlay(MinecraftClient client) {
        if (client.player == null || client.world == null) return "";

        String coords;
        if (TutorialMod.CONFIG.showAccurateCoordinates) {
            coords = String.format("%.3f %.3f %.3f", client.player.getX(), client.player.getY(), client.player.getZ());
        } else {
            coords = String.format("%d %d %d", (int) Math.floor(client.player.getX()), (int) Math.floor(client.player.getY()), (int) Math.floor(client.player.getZ()));
        }

        String facing = "";
        try {
            Direction d = client.player.getHorizontalFacing();
            if (d != null) {
                if (TutorialMod.CONFIG.showDetailedCardinals) {
                    facing = getDetailedFacing(client.player);
                } else {
                    facing = d.toString().substring(0, 1).toUpperCase() + d.toString().substring(1).toLowerCase();
                }
            }
        } catch (Exception ignored) {}

        StringBuilder result = new StringBuilder();
        result.append("Coords: ").append(coords);

        if (TutorialMod.CONFIG.showNetherCoords) {
            String converted = getConvertedCoords(client, client.player.getX(), client.player.getY(), client.player.getZ());
            if (converted != null) {
                result.append(" ").append(converted);
            }
        }

        result.append("\\nFacing: ").append(facing);

        if (TutorialMod.CONFIG.showChunkCount) {
            int completed = client.worldRenderer.getCompletedChunkCount();
            result.append(" C: ").append(completed);
        }

        if (TutorialMod.CONFIG.showEntityCount && client.world != null) {
            int entityCount = 0;
            for (Entity ignored : client.world.getEntities()) {
                entityCount++;
            }
            result.append(" E: ").append(entityCount);
        }

        if (TutorialMod.CONFIG.showLongCoords) {
            pointingTickCounter++;
            if (pointingTickCounter % 2 == 0 || lastLongCoordsInfo == null) {
                lastLongCoordsInfo = getLongCoordsInfo(client);
            }
            if (lastLongCoordsInfo != null) {
                result.append("\\n").append(lastLongCoordsInfo);
            }
        } else {
            lastLongCoordsInfo = null;
            pointingTickCounter = 0;
        }

        if (TutorialMod.CONFIG.showSprintModeOverlay || TutorialMod.CONFIG.showSneakModeOverlay) {
            result.append("\\n");
            if (TutorialMod.CONFIG.showSprintModeOverlay) {
                String mode = ((GameOptionsAccessor) client.options).getSprintToggled().getValue() ? "Toggle" : "Hold";
                result.append("Sprint: ").append(mode);
            }
            if (TutorialMod.CONFIG.showSneakModeOverlay) {
                String mode = ((GameOptionsAccessor) client.options).getSneakToggled().getValue() ? "Toggle" : "Hold";
                if (TutorialMod.CONFIG.showSprintModeOverlay) {
                    result.append(" | ");
                }
                result.append("Sneak: ").append(mode);
            }
        }

        if (TutorialMod.CONFIG.showLatestToggleOverlay && overlayStatusMessage != null) {
            if (System.currentTimeMillis() - overlayStatusTime <= 2000) {
                result.append("\\n").append(overlayStatusMessage);
            } else {
                overlayStatusMessage = null;
            }
        }

        return result.toString();
    }

    private String getLongCoordsInfo(MinecraftClient client) {
        if (client.player == null || client.world == null) return null;

        Vec3d origin = client.player.getCameraPosVec(1.0f);
        Vec3d direction = client.player.getRotationVec(1.0f);
        double maxDist = TutorialMod.CONFIG.longCoordsMaxDistance;

        Entity closestEntity = null;
        double minEntityDist = maxDist;

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            net.minecraft.util.math.Box box = entity.getBoundingBox().expand(0.05);
            java.util.Optional<Vec3d> hit = box.raycast(origin, origin.add(direction.multiply(maxDist)));
            if (hit.isPresent()) {
                double dist = origin.distanceTo(hit.get());
                if (dist < minEntityDist) {
                    minEntityDist = dist;
                    closestEntity = entity;
                }
            }
        }

        BlockPos hitPos = null;
        double step = 0.25;
        double blockDist = -1;

        for (double d = 0; d <= maxDist; d += step) {
            Vec3d point = origin.add(direction.multiply(d));
            BlockPos pos = BlockPos.ofFloored(point);

            if (!client.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
                break;
            }

            BlockState state = client.world.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                hitPos = pos;
                blockDist = d;
                break;
            }
        }

        if (closestEntity != null && (hitPos == null || minEntityDist < blockDist)) {
            String base = String.format("Pointing: %s", closestEntity.getName().getString());
            if (TutorialMod.CONFIG.showNetherCoords) {
                String converted = getConvertedCoords(client, closestEntity.getX(), closestEntity.getY(), closestEntity.getZ());
                if (converted != null) {
                    base += " " + converted;
                }
            }
            if (TutorialMod.CONFIG.showLongCoordsDistance) {
                base += String.format(" D: %d", (int)Math.round(minEntityDist));
            }
            return base;
        }

        if (hitPos != null) {
            String base = String.format("Pointing: %d %d %d", hitPos.getX(), hitPos.getY(), hitPos.getZ());
            if (TutorialMod.CONFIG.showNetherCoords) {
                String converted = getConvertedCoords(client, hitPos.getX(), hitPos.getY(), hitPos.getZ());
                if (converted != null) {
                    base += " " + converted;
                }
            }
            if (TutorialMod.CONFIG.showLongCoordsDistance) {
                base += String.format(" D: %d", (int)Math.round(blockDist));
            }
            return base;
        }

        return "Pointing: None";
    }

    private String getConvertedCoords(MinecraftClient client, double x, double y, double z) {
        if (client.world == null) return null;
        var dim = client.world.getRegistryKey();
        if (dim == World.OVERWORLD) {
            return String.format("N: %d %d %d", (int)Math.floor(x / 8.0), (int)Math.floor(y), (int)Math.floor(z / 8.0));
        } else if (dim == World.NETHER) {
            return String.format("O: %d %d %d", (int)Math.floor(x * 8.0), (int)Math.floor(y), (int)Math.floor(z * 8.0));
        }
        return null;
    }

    private String getDetailedFacing(PlayerEntity player) {
        float yaw = player.getYaw() % 360;
        if (yaw < 0) yaw += 360;

        String cardinal;
        String quadrant;

        if (yaw >= 337.5 || yaw < 22.5) {
            cardinal = "S";
            quadrant = "( _+ )";
        } else if (yaw >= 22.5 && yaw < 67.5) {
            cardinal = "SW";
            quadrant = "( -+ )";
        } else if (yaw >= 67.5 && yaw < 112.5) {
            cardinal = "W";
            quadrant = "( -_ )";
        } else if (yaw >= 112.5 && yaw < 157.5) {
            cardinal = "NW";
            quadrant = "( -- )";
        } else if (yaw >= 157.5 && yaw < 202.5) {
            cardinal = "N";
            quadrant = "( _- )";
        } else if (yaw >= 202.5 && yaw < 247.5) {
            cardinal = "NE";
            quadrant = "( +- )";
        } else if (yaw >= 247.5 && yaw < 292.5) {
            cardinal = "E";
            quadrant = "( +_ )";
        } else {
            cardinal = "SE";
            quadrant = "( ++ )";
        }

        return cardinal + " " + quadrant;
    }

    public static boolean isKeyDown(String translationKey) {
        if (translationKey == null || translationKey.isEmpty() || translationKey.equals("key.keyboard.unknown")) return false;
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(translationKey);
            MinecraftClient mc = MinecraftClient.getInstance();
            if (key.getCategory() == InputUtil.Type.KEYSYM) {
                return InputUtil.isKeyPressed(mc.getWindow(), key.getCode());
            } else if (key.getCategory() == InputUtil.Type.MOUSE) {
                return org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key.getCode()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            }
        } catch (Exception ignored) {}
        return false;
    }

    public boolean isAnyHotbarMeleeKeyHeld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return false;
        for (int i = 0; i < 9; i++) {
            if (isKeyDown(mc.options.hotbarKeys[i].getBoundKeyTranslationKey())) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (isMeleeWeapon(stack)) return true;
            }
        }
        return false;
    }

    public void triggerSprintReset() {
        if (!TutorialMod.CONFIG.sprintResetEnabled || sprintResetCooldownTimer > 0) return;
        sprintResetTimer = TutorialMod.CONFIG.sprintResetDelay;
        sprintResetCooldownTimer = TutorialMod.CONFIG.sprintResetCooldown;
    }

    public boolean isBuildingRecently() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null || lastBlockPlaceTick == -1) return false;
        return mc.world.getTime() - lastBlockPlaceTick < 20;
    }

    private void handleIceGhostSwapTick(MinecraftClient client) {
        if (iceGhostSwapTicks > 0) {
            iceGhostSwapTicks--;
            if (iceGhostSwapTicks == 0) {
                if (iceGhostRestoreSlot != -1) {
                    syncSlot(iceGhostRestoreSlot);
                }
                iceGhostSwapTicks = -1;
                iceGhostRestoreSlot = -1;
            }
        }
    }

    public void handleSprintResetInput(net.minecraft.client.input.Input input) {
        if (autoCritTimer > 0) {
            autoCritTimer--;
            net.minecraft.util.PlayerInput old = input.playerInput;
            input.playerInput = new net.minecraft.util.PlayerInput(false, old.backward(), old.left(), old.right(), old.jump(), old.sneak(), old.sprint());

            float leftImpulse = 0.0f;
            if (old.left()) leftImpulse++;
            if (old.right()) leftImpulse--;

            ((net.rev.tutorialmod.mixin.InputAccessor) input).setMovementVector(new net.minecraft.util.math.Vec2f(leftImpulse, 0.0f));
        }

        if (sprintResetTimer > 0) {
            net.minecraft.util.PlayerInput old = input.playerInput;
            boolean forward = false;
            boolean backward = old.backward();

            if (TutorialMod.CONFIG.sprintResetMode.equals("S-Tap")) {
                backward = true;
            }

            input.playerInput = new net.minecraft.util.PlayerInput(forward, backward, old.left(), old.right(), old.jump(), old.sneak(), old.sprint());

            float forwardImpulse = 0.0f;
            if (forward) forwardImpulse++;
            if (backward) forwardImpulse--;

            float leftImpulse = 0.0f;
            if (old.left()) leftImpulse++;
            if (old.right()) leftImpulse--;

            ((net.rev.tutorialmod.mixin.InputAccessor) input).setMovementVector(new net.minecraft.util.math.Vec2f(leftImpulse, forwardImpulse));
        }
    }
}
