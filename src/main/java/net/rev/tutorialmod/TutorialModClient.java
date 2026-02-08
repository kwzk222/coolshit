package net.rev.tutorialmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
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
import net.rev.tutorialmod.event.AttackEntityCallback;
import net.rev.tutorialmod.mixin.GameOptionsAccessor;
import net.rev.tutorialmod.mixin.MinecraftClientAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.rev.tutorialmod.modules.AutoTotem;
import net.rev.tutorialmod.modules.EnemyInfo;
import net.rev.tutorialmod.modules.OverlayManager;
import net.rev.tutorialmod.modules.PotionModule;
import net.rev.tutorialmod.modules.TriggerBot;
import net.rev.tutorialmod.modules.misc.ClickSpamModule;
import net.rev.tutorialmod.modules.movement.BridgeAssistModule;
import net.rev.tutorialmod.modules.movement.ClutchModule;
import net.rev.tutorialmod.modules.movement.JumpResetModule;
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
    private AutoTotem autoTotem;
    private EnemyInfo enemyInfo;
    private PotionModule potionModule;
    private ParkourModule parkourModule;
    private JumpResetModule jumpResetModule;
    private ClutchModule clutchModule;
    private BridgeAssistModule bridgeAssistModule;
    private static OverlayManager overlayManager;

    public AutoTotem getAutoTotem() {
        return autoTotem;
    }

    public static OverlayManager getOverlayManager() {
        return overlayManager;
    }

    public EnemyInfo getEnemyInfo() {
        return enemyInfo;
    }

    public JumpResetModule getJumpResetModule() {
        return jumpResetModule;
    }


    // --- Keybind States ---
    private boolean openSettingsWasPressed = false;
    private boolean masterToggleWasPressed = false;
    private boolean teammateWasPressed = false;
    private boolean triggerBotToggleWasPressed = false;
    private boolean overlayToggleWasPressed = false;
    private boolean parkourToggleWasPressed = false;
    private boolean clutchToggleWasPressed = false;
    private boolean miningResetWasPressed = false;
    private boolean sprintModeWasPressed = false;
    private boolean sneakModeWasPressed = false;

    // --- State: Combat Swap ---
    private enum SwapAction { NONE, SWITCH_BACK, SWITCH_TO_ORIGINAL_THEN_MACE, SWITCH_BACK_FROM_MACE }
    private int swapCooldown = -1;
    private int originalHotbarSlot = -1;
    private SwapAction nextAction = SwapAction.NONE;
    private Entity targetEntity = null;

    // --- State: Placement Sequence (TNT Minecart, etc.) ---
    private enum PlacementAction { NONE, PLACE_TNT_MINECART, AWAITING_LAVA_PLACEMENT, AWAITING_FIRE_PLACEMENT, SWITCH_TO_CROSSBOW, SWITCH_TO_BOW }
    private int placementCooldown = -1;
    private PlacementAction nextPlacementAction = PlacementAction.NONE;
    private BlockPos railPos = null;
    private int utilitySlot = -1;
    private int crossbowSlot = -1;
    private int actionTimeout = -1;
    private int minecartRetryCounter = 0;

    // --- State: Misc ---
    public static long lastBowShotTick = -1;
    public static int awaitingRailConfirmationCooldown = -1;
    public static int awaitingMinecartConfirmationCooldown = -1;

    private int pointingTickCounter = 0;
    private String lastLongCoordsInfo = null;

    private int crossbowUseTicks = 0;
    private boolean crossbowWasUsing = false;

    private String overlayStatusMessage = null;
    private long overlayStatusTime = 0;

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
        autoTotem = new AutoTotem();
        enemyInfo = new EnemyInfo();
        potionModule = new PotionModule();
        parkourModule = new ParkourModule();
        jumpResetModule = new JumpResetModule();
        clutchModule = new ClutchModule();
        bridgeAssistModule = new BridgeAssistModule();
        overlayManager = new OverlayManager();
        autoTotem.init();
        parkourModule.init();
        bridgeAssistModule.init();

        // Add shutdown hook to stop overlay process
        Runtime.getRuntime().addShutdownHook(new Thread(overlayManager::stop));

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
        // Handle keybinds first, as they might toggle features on/off.
        handleKeybinds(client);
        handleQuickCrossbow(client);
        handleChatMacros(client);

        // Handle TriggerBot separately, as it may have its own master toggle.
        if (triggerBot != null) {
            triggerBot.onTick();
        }

        // Handle Enemy Info Ticks
        if (TutorialMod.CONFIG.showEnemyInfo) {
            enemyInfo.onTick(client);
        }

        // --- Centralized Overlay Logic ---
        boolean shouldOverlayBeRunning = TutorialMod.CONFIG.showCoordsOverlay || TutorialMod.CONFIG.showEnemyInfo;
        if (shouldOverlayBeRunning && !overlayManager.isRunning() && client.player != null) {
            overlayManager.start();
        } else if ((!shouldOverlayBeRunning || client.player == null) && overlayManager.isRunning()) {
            overlayManager.stop();
        }

        if (overlayManager.isRunning() && client.player != null) {
            String enemyInfoString = TutorialMod.CONFIG.showEnemyInfo ? enemyInfo.getFormattedEnemyInfo() : null;
            if (enemyInfoString != null) {
                overlayManager.update(enemyInfoString);
            } else if (TutorialMod.CONFIG.showCoordsOverlay) {
                overlayManager.update(formatCoordsForOverlay(client));
            } else {
                overlayManager.update(""); // Clear/Hide overlay
            }
        }


        // Handle AutoToolSwitch tick
        TutorialMod.getAutoToolSwitch().onTick();

        // --- Feature Ticks ---
        // These are handled even if master is disabled to ensure state is cleaned up or updated.
        handleCombatSwap(client);
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

        if (jumpResetModule != null) {
            jumpResetModule.onTick(client);
        }

        ClickSpamModule.onTick();
    }


    private ActionResult onAttackEntity(PlayerEntity player, Entity target) {
        if (!TutorialMod.CONFIG.masterEnabled) return ActionResult.PASS;
        if (target instanceof PlayerEntity attackedPlayer) {
            ItemStack held = player.getMainHandStack();
            boolean isSpear = held.isIn(ItemTags.SPEARS);
            boolean isMace = held.getItem() == Items.MACE;
            boolean isSword = held.isIn(ItemTags.SWORDS);

            boolean isShielding = attackedPlayer.isUsingItem() && attackedPlayer.getActiveItem().getItem() == Items.SHIELD;

            boolean isFacing = true;
            if (TutorialMod.CONFIG.autoStunFacingCheck) {
                Vec3d selfPos = new Vec3d(player.getX(), player.getY(), player.getZ());
                Vec3d targetPos = new Vec3d(attackedPlayer.getX(), attackedPlayer.getY(), attackedPlayer.getZ());
                Vec3d targetLookVec = attackedPlayer.getRotationVector();
                Vec3d vecToSelf = selfPos.subtract(targetPos).normalize();
                isFacing = vecToSelf.dotProduct(targetLookVec) > 0;
            }

            boolean hasArmor = isArmored(attackedPlayer);
            PlayerInventoryMixin inventory = (PlayerInventoryMixin) player.getInventory();

            boolean hasShield = attackedPlayer.getMainHandStack().getItem() == Items.SHIELD || attackedPlayer.getOffHandStack().getItem() == Items.SHIELD;

            // AutoStun Trigger Determination
            boolean attemptStun = false;
            int stunFailChance = 0;
            int stunPredictionChance = 0;
            double stunRange = 3.1;
            int stunDelay = 1;
            boolean stunEnabled = false;

            if (isSpear) {
                stunEnabled = TutorialMod.CONFIG.spearAutoStunEnabled;
                stunRange = TutorialMod.CONFIG.spearAutoStunRange;
                stunFailChance = TutorialMod.CONFIG.spearAutoStunFailChance;
                stunPredictionChance = TutorialMod.CONFIG.spearAutoStunFakePredictionChance;
                stunDelay = TutorialMod.CONFIG.spearAutoStunDelay;
            } else if (isMace) {
                stunEnabled = TutorialMod.CONFIG.maceAutoStunEnabled;
                stunRange = TutorialMod.CONFIG.maceAutoStunRange;
                stunFailChance = TutorialMod.CONFIG.maceAutoStunFailChance;
                stunPredictionChance = TutorialMod.CONFIG.maceAutoStunFakePredictionChance;
                stunDelay = TutorialMod.CONFIG.maceAutoStunDelay;
            } else if (isSword) {
                stunEnabled = TutorialMod.CONFIG.axeSwapEnabled;
                stunRange = TutorialMod.CONFIG.axeSwapRange;
                stunFailChance = TutorialMod.CONFIG.axeSwapFailChance;
                stunPredictionChance = TutorialMod.CONFIG.axeSwapFakePredictionChance;
                stunDelay = TutorialMod.CONFIG.axeSwapDelay;
            }

            boolean fakePrediction = hasShield && !isShielding && (RANDOM.nextInt(100) < stunPredictionChance);
            attemptStun = stunEnabled && ((isShielding && isFacing) || fakePrediction);

            if (attemptStun) {
                if (stunFailChance > 0 && RANDOM.nextInt(100) < stunFailChance) return ActionResult.PASS;

                double dist = player.distanceTo(attackedPlayer);
                if (dist > stunRange) return ActionResult.PASS;

                int axeSlot = findAxeInHotbar(player);
                if (axeSlot != -1 && inventory.getSelectedSlot() != axeSlot) {
                    if (swapCooldown == -1) {
                        originalHotbarSlot = inventory.getSelectedSlot();
                    }
                    syncSlot(axeSlot);
                    targetEntity = target;
                    int maceSlot = findMaceInHotbar(player);
                    if (hasArmor && maceSlot != -1 && TutorialMod.CONFIG.maceSwapEnabled && player.fallDistance > TutorialMod.CONFIG.maceSwapMinFallDistance) {
                        swapCooldown = TutorialMod.CONFIG.axeToOriginalDelay;
                        nextAction = SwapAction.SWITCH_TO_ORIGINAL_THEN_MACE;
                    } else {
                        swapCooldown = stunDelay;
                        nextAction = SwapAction.SWITCH_BACK;
                    }
                    return ActionResult.PASS; // Return PASS so the attack continues with the new Axe item
                }
            }

            // Mace Attribute Swap (Damage)
            if (hasArmor && TutorialMod.CONFIG.maceSwapEnabled && player.fallDistance > TutorialMod.CONFIG.maceSwapMinFallDistance) {
                if (TutorialMod.CONFIG.maceSwapFailChance > 0 && RANDOM.nextInt(100) < TutorialMod.CONFIG.maceSwapFailChance) return ActionResult.PASS;
                if (player.distanceTo(attackedPlayer) > TutorialMod.CONFIG.maceSwapRange) return ActionResult.PASS;

                int maceSlot = findMaceInHotbar(player);
                if (maceSlot != -1 && inventory.getSelectedSlot() != maceSlot) {
                    if (swapCooldown == -1) {
                        originalHotbarSlot = inventory.getSelectedSlot();
                    }
                    syncSlot(maceSlot);
                    swapCooldown = TutorialMod.CONFIG.maceSwapDelay;
                    nextAction = SwapAction.SWITCH_BACK;
                }
            }
        }
        return ActionResult.PASS;
    }


    private void syncSlot(int slot) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
        inventory.setSelectedSlot(slot);
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    private void handleKeybinds(MinecraftClient client) {
        if (client.player == null) return;

        if (!TutorialMod.CONFIG.activeInInventory && client.currentScreen != null) {
            return;
        }

        // --- Open Settings Hotkey ---
        try {
            boolean isOpenSettingsPressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.openSettingsHotkey).getCode());
            if (isOpenSettingsPressed && !openSettingsWasPressed) {
                client.setScreen(new ModMenuIntegration().getModConfigScreenFactory().create(client.currentScreen));
            }
            openSettingsWasPressed = isOpenSettingsPressed;
        } catch (IllegalArgumentException e) {
            // Invalid key
        }


        // --- Toggle Clutch Hotkey ---
        try {
            boolean isClutchTogglePressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.clutchHotkey).getCode());
            if (isClutchTogglePressed && !clutchToggleWasPressed) {
                TutorialMod.CONFIG.clutchEnabled = !TutorialMod.CONFIG.clutchEnabled;
                TutorialMod.CONFIG.save();
                TutorialMod.sendUpdateMessage("Clutch set to " + (TutorialMod.CONFIG.clutchEnabled ? "ON" : "OFF"));
            }
            clutchToggleWasPressed = isClutchTogglePressed;
        } catch (IllegalArgumentException e) {
            // Invalid key
        }

        try {
            boolean isMasterTogglePressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.masterToggleHotkey).getCode());
            if (isMasterTogglePressed && !masterToggleWasPressed) {
                TutorialMod.CONFIG.masterEnabled = !TutorialMod.CONFIG.masterEnabled;
                TutorialMod.CONFIG.save();
                TutorialMod.sendUpdateMessage("Master Switch set to " + (TutorialMod.CONFIG.masterEnabled ? "ON" : "OFF"));
            }
            masterToggleWasPressed = isMasterTogglePressed;
        } catch (IllegalArgumentException e) {
            // This can happen if the key is not set or invalid.
        }


        try {
            boolean isTeammateKeyPressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.teammateHotkey).getCode());
            if (isTeammateKeyPressed && !teammateWasPressed) {
                handleTeammateKeybind(client);
            }
            teammateWasPressed = isTeammateKeyPressed;
        } catch (IllegalArgumentException e) {
        }


        try {
            boolean isTriggerBotTogglePressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.triggerBotToggleHotkey).getCode());
            if (isTriggerBotTogglePressed && !triggerBotToggleWasPressed) {
                TutorialMod.CONFIG.triggerBotToggledOn = !TutorialMod.CONFIG.triggerBotToggledOn;
                TutorialMod.sendUpdateMessage("TriggerBot set to " + (TutorialMod.CONFIG.triggerBotToggledOn ? "ON" : "OFF"));
            }
            triggerBotToggleWasPressed = isTriggerBotTogglePressed;
        } catch (IllegalArgumentException e) {
        }

        // --- Toggle Overlay Hotkey ---
        try {
            boolean isToggleOverlayPressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.toggleOverlayHotkey).getCode());
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
        } catch (IllegalArgumentException e) {
            // Invalid key
        }

        // --- Toggle Parkour Hotkey ---
        try {
            boolean isParkourTogglePressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.parkourHotkey).getCode());
            if (isParkourTogglePressed && !parkourToggleWasPressed) {
                parkourModule.toggle();
                TutorialMod.sendUpdateMessage("Parkour set to " + (TutorialMod.CONFIG.parkourEnabled ? "ON" : "OFF"));
            }
            parkourToggleWasPressed = isParkourTogglePressed;
        } catch (IllegalArgumentException e) {
            // Invalid key
        }

        // --- Toggle Sprint Mode Hotkey ---
        try {
            boolean isSprintModePressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.sprintModeHotkey).getCode());
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
        } catch (IllegalArgumentException e) {
        }

        // --- Toggle Sneak Mode Hotkey ---
        try {
            boolean isSneakModePressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.sneakModeHotkey).getCode());
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
        } catch (IllegalArgumentException e) {
        }
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

    private void handleCombatSwap(MinecraftClient client) {
        if (swapCooldown > 0) {
            swapCooldown--;
        } else if (swapCooldown == 0) {
            SwapAction action = nextAction;
            nextAction = SwapAction.NONE;
            swapCooldown = -1;
            if (client.player == null) return;
            switch (action) {
                case SWITCH_BACK:
                    syncSlot(originalHotbarSlot);
                    break;
                case SWITCH_TO_ORIGINAL_THEN_MACE:
                    syncSlot(originalHotbarSlot);
                    if (client.interactionManager != null && targetEntity != null && targetEntity.isAlive()) {
                        client.interactionManager.attackEntity(client.player, targetEntity);
                    }
                    int maceSlot = findMaceInHotbar(client.player);
                    if (maceSlot != -1) {
                        syncSlot(maceSlot);
                        swapCooldown = TutorialMod.CONFIG.maceToOriginalDelay;
                        nextAction = SwapAction.SWITCH_BACK_FROM_MACE;
                    }
                    break;
                case SWITCH_BACK_FROM_MACE:
                    syncSlot(originalHotbarSlot);
                    break;
                default: break;
            }
            targetEntity = null;
        }
    }

    private void handlePlacementSequence(MinecraftClient client) {
        if (actionTimeout > 0) {
            actionTimeout--;
            if (client.player != null && ((PlayerInventoryMixin) client.player.getInventory()).getSelectedSlot() != utilitySlot) {
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
                        // Simulated right click only if looking at a rail
                        if (client.crosshairTarget instanceof BlockHitResult bhr) {
                            BlockState state = client.world.getBlockState(bhr.getBlockPos());
                            if (state.getBlock() instanceof net.minecraft.block.AbstractRailBlock) {
                                syncSlot(minecartSlot);
                                ((MinecraftClientAccessor) client).setItemUseCooldown(0);
                                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, bhr);
                                client.player.swingHand(Hand.MAIN_HAND);
                                awaitingMinecartConfirmationCooldown = 60; // Wait for server confirmation
                                placementCooldown = -1;
                                nextPlacementAction = PlacementAction.NONE;
                                minecartRetryCounter = 0;
                                return;
                            }
                        }

                        if (minecartRetryCounter < 10) {
                            minecartRetryCounter++;
                            placementCooldown = 1; // Retry next tick
                            nextPlacementAction = action;
                        } else {
                            // Timeout/Fail
                            placementCooldown = -1;
                            nextPlacementAction = PlacementAction.NONE;
                            minecartRetryCounter = 0;
                            railPos = null;
                        }
                    }
                    break;
                case AWAITING_LAVA_PLACEMENT:
                case AWAITING_FIRE_PLACEMENT:
                    if (actionTimeout == 0) {
                        utilitySlot = -1;
                        crossbowSlot = -1;
                    } else {
                        placementCooldown = 1;
                        nextPlacementAction = action;
                    }
                    break;
                case SWITCH_TO_CROSSBOW:
                    inventory.setSelectedSlot(crossbowSlot);
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
        /* NOTE: Server confirmation logic - disabled to fix "cart tech" on laggy servers
        if (awaitingRailConfirmationCooldown > 0 && state.getBlock() instanceof net.minecraft.block.AbstractRailBlock) {
            if (instance != null) instance.startRailPlacement(pos);
            awaitingRailConfirmationCooldown = -1;
        }
        */
    }

    public static void confirmLavaPlacement(BlockPos pos, BlockState state) {
        if (instance != null && instance.nextPlacementAction == PlacementAction.AWAITING_LAVA_PLACEMENT && state.getBlock() == net.minecraft.block.Blocks.LAVA) {
            instance.placementCooldown = 1;
            instance.nextPlacementAction = PlacementAction.SWITCH_TO_CROSSBOW;
            instance.actionTimeout = -1;
        }
    }

    public static void confirmFirePlacement(BlockPos pos, BlockState state) {
        if (instance != null && instance.nextPlacementAction == PlacementAction.AWAITING_FIRE_PLACEMENT && state.getBlock() == net.minecraft.block.Blocks.FIRE) {
            instance.placementCooldown = 1;
            instance.nextPlacementAction = PlacementAction.SWITCH_TO_CROSSBOW;
            instance.actionTimeout = -1;
        }
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
        this.minecartRetryCounter = 0;
        this.nextPlacementAction = PlacementAction.PLACE_TNT_MINECART;
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
                    inventory.setSelectedSlot(lavaSlot);
                    this.actionTimeout = 60;
                    this.placementCooldown = 1;
                    this.nextPlacementAction = PlacementAction.AWAITING_LAVA_PLACEMENT;
                    return;
                }
                int flintSlot = findFlintAndSteelInHotbar(client.player);
                if (flintSlot != -1) {
                    this.utilitySlot = flintSlot;
                    this.crossbowSlot = crossSlot;
                    inventory.setSelectedSlot(flintSlot);
                    this.actionTimeout = 60;
                    this.placementCooldown = 1;
                    this.nextPlacementAction = PlacementAction.AWAITING_FIRE_PLACEMENT;
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

    public void onReachSwap() {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.spearReachSwapEnabled || swapCooldown != -1) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.interactionManager == null) return;

        ItemStack held = client.player.getMainHandStack();
        boolean holdingSpear = held.isIn(ItemTags.SPEARS);

        // Check if there is a target within spear reach (4.0) but beyond current reach (3.0)
        Entity target = getEntityLookingAt(client, TutorialMod.CONFIG.spearReachSwapRange, TutorialMod.CONFIG.reachSwapIgnoreCobwebs);
        if (target != null) {
            double dist = client.player.distanceTo(target);
            if (dist > TutorialMod.CONFIG.reachSwapActivationRange) {
                if (holdingSpear) {
                    // Already holding spear, just attack
                    client.interactionManager.attackEntity(client.player, target);
                    client.player.swingHand(Hand.MAIN_HAND);
                } else {
                    int spearSlot = findSpearInHotbar(client.player);
                    if (spearSlot != -1) {
                        PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
                        originalHotbarSlot = inventory.getSelectedSlot();
                        syncSlot(spearSlot);

                        // IMPORTANT: Set cooldown and action BEFORE calling attackEntity to prevent slot corruption
                        // if AutoStun/Mace triggers nestedly.
                        swapCooldown = TutorialMod.CONFIG.reachSwapBackDelay;
                        nextAction = SwapAction.SWITCH_BACK;

                        // Save current nextAction to see if it changes
                        SwapAction previousAction = nextAction;

                        // Trigger manual attack in the same tick
                        client.interactionManager.attackEntity(client.player, target);
                        client.player.swingHand(Hand.MAIN_HAND);

                        // If the nested attack started a complex sequence (e.g. Mace Swap), don't overwrite it
                        if (nextAction != previousAction && nextAction != SwapAction.NONE) {
                            // Complex sequence active, keep it.
                        } else {
                            // Ensure it's still set
                            nextAction = SwapAction.SWITCH_BACK;
                        }
                    }
                }
            }
        }
    }

    private boolean isKeyCurrentlyPressed(net.minecraft.client.option.KeyBinding keyBinding, MinecraftClient client) {
        try {
            return InputUtil.isKeyPressed(client.getWindow(),
                InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
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
        boolean isCrossbow = mainHand.getItem() == Items.CROSSBOW;
        boolean isEmpty = isCrossbow && !CrossbowItem.isCharged(mainHand);

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

    public int findSpearInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).isIn(ItemTags.SPEARS)) return i;
        }
        return -1;
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
            if (player.getInventory().getStack(i).getItem() == Items.FLINT_AND_STEEL) return i;
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
        // Limit iterations to prevent infinite loops in weird edge cases
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
                // Move start point past the cobweb
                currentStart = hit.getPos().add(end.subtract(start).normalize().multiply(0.01));
            } else {
                // Check if the hit position is significantly before the target
                return hit.getPos().distanceTo(start) < end.distanceTo(start) - 0.05;
            }
        }
        return false;
    }

    private PlayerEntity getPlayerLookingAt(MinecraftClient client, double maxDistance) {
        Entity entity = getEntityLookingAt(client, maxDistance, false);
        return (entity instanceof PlayerEntity) ? (PlayerEntity) entity : null;
    }

    private void handleChatMacros(MinecraftClient client) {
        if (client.player == null) return;

        List<ModConfig.Macro> macros = new ArrayList<>(Arrays.asList(TutorialMod.CONFIG.macro1, TutorialMod.CONFIG.macro2, TutorialMod.CONFIG.macro3, TutorialMod.CONFIG.macro4, TutorialMod.CONFIG.macro5));

        for (ModConfig.Macro macro : macros) {
            if (macro.hotkey == null || macro.hotkey.equals("key.keyboard.unknown") || macro.message == null || macro.message.isEmpty()) {
                continue;
            }

            boolean isPressed;
            try {
                isPressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(macro.hotkey).getCode());
            } catch (IllegalArgumentException e) {
                continue; // Invalid key
            }

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

        // Dimension identifier
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

        // Facing (cardinal)
        String facing = "";
        try {
            Direction d = player.getHorizontalFacing();
            if (d != null) facing = " " + d.toString().toLowerCase();
        } catch (Exception ignored) {
        }

        // Compose replacements
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
                    // Capitalize first letter
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

        // Check entities first
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

        // March the ray for blocks
        BlockPos hitPos = null;
        double step = 0.25;
        double blockDist = -1;

        for (double d = 0; d <= maxDist; d += step) {
            Vec3d point = origin.add(direction.multiply(d));
            BlockPos pos = BlockPos.ofFloored(point);

            // Manual check for unloaded chunks to avoid lag/ghost blocks
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

}
