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
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.event.AttackEntityCallback;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.rev.tutorialmod.modules.AutoTotem;
import net.rev.tutorialmod.modules.EnemyInfo;
import net.rev.tutorialmod.modules.OverlayManager;
import net.rev.tutorialmod.modules.TriggerBot;

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


    // --- Keybind States ---
    private boolean openSettingsWasPressed = false;
    private boolean masterToggleWasPressed = false;
    private boolean teammateWasPressed = false;
    private boolean triggerBotToggleWasPressed = false;
    private boolean toggleSneakWasPressed = false;
    private boolean toggleSprintWasPressed = false;
    private boolean overlayToggleWasPressed = false;

    private static int clickCooldown = -1;

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

    // --- State: Misc ---
    public static long lastBowShotTick = -1;
    public static int awaitingRailConfirmationCooldown = -1;


    @Override
    public void onInitializeClient() {
        instance = this;
        triggerBot = new TriggerBot();
        autoTotem = new AutoTotem();
        enemyInfo = new EnemyInfo();
        overlayManager = new OverlayManager();
        autoTotem.init();

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
        handleChatMacros(client);

        // Handle TriggerBot separately, as it may have its own master toggle.
        if (triggerBot != null) {
            triggerBot.onTick(client);
        }

        // Handle Enemy Info Ticks
        if (TutorialMod.CONFIG.showEnemyInfo) {
            enemyInfo.onTick(client);
        }

        // --- Centralized Overlay Logic ---
        if (overlayManager.isRunning() && client.player != null) {
            String enemyInfoString = TutorialMod.CONFIG.showEnemyInfo ? enemyInfo.getFormattedEnemyInfo() : null;
            if (enemyInfoString != null) {
                overlayManager.update(enemyInfoString);
            } else if (TutorialMod.CONFIG.showCoordsOverlay) {
                overlayManager.update(formatCoordsForOverlay(client));
            } else {
                overlayManager.update(""); // Clear overlay
            }
        }


        // Handle AutoToolSwitch tick
        TutorialMod.getAutoToolSwitch().onTick();

        // --- Toggles ---
        // This is handled here to ensure toggles can be turned off even if the master switch is disabled.
        handleToggleKeys(client);

        // Master toggle check for all subsequent features.
        if (!TutorialMod.CONFIG.masterEnabled) return;

        // --- Feature Ticks ---
        if (TutorialMod.CONFIG.autoTotemEnabled) {
            autoTotem.onTick(client);
        }
        handlePlacementClick(client);
        handleCombatSwap(client);
        handlePlacementSequence(client);
        handleConfirmationCooldowns(client);
    }

    private ActionResult onAttackEntity(PlayerEntity player, Entity target) {
        if (!TutorialMod.CONFIG.masterEnabled || swapCooldown != -1) return ActionResult.PASS;
        if (target instanceof PlayerEntity) {
            PlayerEntity attackedPlayer = (PlayerEntity) target;
            boolean isShielding = attackedPlayer.isUsingItem() && attackedPlayer.getActiveItem().getItem() == Items.SHIELD;

            Vec3d selfPos = player.getPos();
            Vec3d targetPos = attackedPlayer.getPos();
            Vec3d targetLookVec = attackedPlayer.getRotationVector();
            Vec3d vecToSelf = selfPos.subtract(targetPos).normalize();
            boolean isFacing = vecToSelf.dotProduct(targetLookVec) > 0;

            boolean hasArmor = isArmored(attackedPlayer);
            PlayerInventoryMixin inventory = (PlayerInventoryMixin) player.getInventory();

            boolean shouldAttemptSwap = (isShielding && isFacing) || (RANDOM.nextInt(100) < TutorialMod.CONFIG.fakePredictionChance);

            if (shouldAttemptSwap && TutorialMod.CONFIG.axeSwapEnabled) {
                if (TutorialMod.CONFIG.axeSwapFailChance > 0 && RANDOM.nextInt(100) < TutorialMod.CONFIG.axeSwapFailChance) {
                    return ActionResult.PASS;
                }
                int axeSlot = findAxeInHotbar(player);
                if (axeSlot != -1 && inventory.getSelectedSlot() != axeSlot) {
                    originalHotbarSlot = inventory.getSelectedSlot();
                    inventory.setSelectedSlot(axeSlot);
                    targetEntity = target;
                    int maceSlot = findMaceInHotbar(player);
                    if (hasArmor && maceSlot != -1 && TutorialMod.CONFIG.maceSwapEnabled && player.fallDistance > TutorialMod.CONFIG.minFallDistance) {
                        swapCooldown = TutorialMod.CONFIG.axeToOriginalDelay;
                        nextAction = SwapAction.SWITCH_TO_ORIGINAL_THEN_MACE;
                    } else {
                        swapCooldown = TutorialMod.CONFIG.axeSwapDelay;
                        nextAction = SwapAction.SWITCH_BACK;
                    }
                }
            } else if (hasArmor && TutorialMod.CONFIG.maceSwapEnabled) {
                int maceSlot = findMaceInHotbar(player);
                if (maceSlot != -1 && inventory.getSelectedSlot() != maceSlot) {
                    originalHotbarSlot = inventory.getSelectedSlot();
                    inventory.setSelectedSlot(maceSlot);
                    swapCooldown = TutorialMod.CONFIG.maceSwapDelay;
                    nextAction = SwapAction.SWITCH_BACK;
                }
            }
        }
        return ActionResult.PASS;
    }

    private void handleKeybinds(MinecraftClient client) {
        if (client.player == null) return;

        if (!TutorialMod.CONFIG.activeInInventory && client.currentScreen != null) {
            return;
        }

        // --- Open Settings Hotkey ---
        try {
            boolean isOpenSettingsPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.openSettingsHotkey).getCode());
            if (isOpenSettingsPressed && !openSettingsWasPressed) {
                client.setScreen(new ModMenuIntegration().getModConfigScreenFactory().create(client.currentScreen));
            }
            openSettingsWasPressed = isOpenSettingsPressed;
        } catch (IllegalArgumentException e) {
            // Invalid key
        }

        try {
            boolean isMasterTogglePressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.masterToggleHotkey).getCode());
            if (isMasterTogglePressed && !masterToggleWasPressed) {
                TutorialMod.CONFIG.masterEnabled = !TutorialMod.CONFIG.masterEnabled;
                TutorialMod.CONFIG.save();
                if (!TutorialMod.CONFIG.disableModChatUpdates) {
                    client.player.sendMessage(Text.of("TutorialMod Master Switch: " + (TutorialMod.CONFIG.masterEnabled ? "ON" : "OFF")), false);
                }
            }
            masterToggleWasPressed = isMasterTogglePressed;
        } catch (IllegalArgumentException e) {
            // This can happen if the key is not set or invalid.
        }


        try {
            boolean isTeammateKeyPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.teammateHotkey).getCode());
            if (isTeammateKeyPressed && !teammateWasPressed) {
                handleTeammateKeybind(client);
            }
            teammateWasPressed = isTeammateKeyPressed;
        } catch (IllegalArgumentException e) {
        }


        try {
            boolean isTriggerBotTogglePressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.triggerBotToggleHotkey).getCode());
            if (isTriggerBotTogglePressed && !triggerBotToggleWasPressed) {
                TutorialMod.CONFIG.triggerBotToggledOn = !TutorialMod.CONFIG.triggerBotToggledOn;
                if (!TutorialMod.CONFIG.disableModChatUpdates) {
                    client.player.sendMessage(Text.of("TriggerBot: " + (TutorialMod.CONFIG.triggerBotToggledOn ? "ON" : "OFF")), false);
                }
            }
            triggerBotToggleWasPressed = isTriggerBotTogglePressed;
        } catch (IllegalArgumentException e) {
        }

        // --- Toggle Sneak Hotkey ---
        try {
            boolean isToggleSneakPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.toggleSneakHotkey).getCode());
            if (isToggleSneakPressed && !toggleSneakWasPressed) {
                TutorialMod.CONFIG.isToggleSneakOn = !TutorialMod.CONFIG.isToggleSneakOn;
                if (!TutorialMod.CONFIG.disableModChatUpdates) {
                    client.player.sendMessage(Text.of("Toggle Sneak: " + (TutorialMod.CONFIG.isToggleSneakOn ? "ON" : "OFF")), false);
                }

                if (TutorialMod.CONFIG.isToggleSneakOn) {
                    if (TutorialMod.CONFIG.isToggleSprintOn) {
                        TutorialMod.CONFIG.isToggleSprintOn = false;
                        if (!TutorialMod.CONFIG.disableModChatUpdates) {
                            client.player.sendMessage(Text.of("Toggle Sprint: OFF"), false);
                        }
                        client.options.sprintKey.setPressed(false);
                    }
                } else {
                    client.options.sneakKey.setPressed(false);
                }
            }
            toggleSneakWasPressed = isToggleSneakPressed;
        } catch (IllegalArgumentException e) {
        }

        // --- Toggle Sprint Hotkey ---
        try {
            boolean isToggleSprintPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.toggleSprintHotkey).getCode());
            if (isToggleSprintPressed && !toggleSprintWasPressed) {
                TutorialMod.CONFIG.isToggleSprintOn = !TutorialMod.CONFIG.isToggleSprintOn;
                if (!TutorialMod.CONFIG.disableModChatUpdates) {
                    client.player.sendMessage(Text.of("Toggle Sprint: " + (TutorialMod.CONFIG.isToggleSprintOn ? "ON" : "OFF")), false);
                }

                if (TutorialMod.CONFIG.isToggleSprintOn) {
                    if (TutorialMod.CONFIG.isToggleSneakOn) {
                        TutorialMod.CONFIG.isToggleSneakOn = false;
                        if (!TutorialMod.CONFIG.disableModChatUpdates) {
                            client.player.sendMessage(Text.of("Toggle Sneak: OFF"), false);
                        }
                        client.options.sneakKey.setPressed(false);
                    }
                } else {
                    client.options.sprintKey.setPressed(false);
                }
            }
            toggleSprintWasPressed = isToggleSprintPressed;
        } catch (IllegalArgumentException e) {
        }

        // --- Toggle Overlay Hotkey ---
        try {
            boolean isToggleOverlayPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.toggleOverlayHotkey).getCode());
            if (isToggleOverlayPressed && !overlayToggleWasPressed) {
                TutorialMod.CONFIG.showCoordsOverlay = !TutorialMod.CONFIG.showCoordsOverlay;
                TutorialMod.CONFIG.save();
                client.player.sendMessage(Text.of("Coords Overlay: " + (TutorialMod.CONFIG.showCoordsOverlay ? "ON" : "OFF")), false);
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
    }

    private void handlePlacementClick(MinecraftClient client) {
        if (clickCooldown > 0) {
            clickCooldown--;
        } else if (clickCooldown == 0) {
            client.options.useKey.setPressed(false);
            clickCooldown = -1;
        }
    }

    private void handleTeammateKeybind(MinecraftClient client) {
        PlayerEntity target = getPlayerLookingAt(client, 9.0);
        if (target != null) {
            String name = target.getName().getString();
            if (TutorialMod.CONFIG.teamManager.isTeammate(name)) {
                TutorialMod.CONFIG.teamManager.removeTeammate(name);
                if (!TutorialMod.CONFIG.disableModChatUpdates) {
                    client.player.sendMessage(Text.of("Removed " + name + " from your team."), false);
                }
            } else {
                if (TutorialMod.CONFIG.teamManager.addTeammate(name)) {
                    if (!TutorialMod.CONFIG.disableModChatUpdates) {
                        client.player.sendMessage(Text.of("Added " + name + " to your team."), false);
                    }
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
            PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
            switch (action) {
                case SWITCH_BACK:
                    inventory.setSelectedSlot(originalHotbarSlot);
                    break;
                case SWITCH_TO_ORIGINAL_THEN_MACE:
                    inventory.setSelectedSlot(originalHotbarSlot);
                    if (client.interactionManager != null && targetEntity != null && targetEntity.isAlive()) {
                        client.interactionManager.attackEntity(client.player, targetEntity);
                    }
                    int maceSlot = findMaceInHotbar(client.player);
                    if (maceSlot != -1) {
                        inventory.setSelectedSlot(maceSlot);
                        swapCooldown = TutorialMod.CONFIG.maceToOriginalDelay;
                        nextAction = SwapAction.SWITCH_BACK_FROM_MACE;
                    }
                    break;
                case SWITCH_BACK_FROM_MACE:
                    inventory.setSelectedSlot(originalHotbarSlot);
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
                    HitResult crosshairTarget = client.crosshairTarget;
                    if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.BLOCK && ((BlockHitResult) crosshairTarget).getBlockPos().equals(railPos)) {
                        inventory.setSelectedSlot(findTntMinecartInHotbar(client.player));
                        BlockHitResult hitResult = new BlockHitResult(railPos.toCenterPos(), Direction.UP, railPos, false);
                        client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
                        startPostMinecartSequence(client);
                    } else {
                        railPos = null;
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
    }

    public static void requestPlacement() {
        if (clickCooldown == -1) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.options.useKey.setPressed(true);
                client.player.swingHand(Hand.MAIN_HAND);
                clickCooldown = 1;
            }
        }
    }

    public static void setAwaitingRailConfirmation() {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.tntMinecartPlacementEnabled) return;
        awaitingRailConfirmationCooldown = 2;
    }

    public static void confirmRailPlacement(BlockPos pos, BlockState state) {
        if (awaitingRailConfirmationCooldown > 0 && state.getBlock() instanceof net.minecraft.block.RailBlock) {
            if (instance != null) instance.startRailPlacement(pos);
            awaitingRailConfirmationCooldown = -1;
        }
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
        this.nextPlacementAction = PlacementAction.PLACE_TNT_MINECART;
    }

    public void startPostMinecartSequence(MinecraftClient client) {
        if (client.player == null) return;
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

    private int findMaceInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.MACE) return i;
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

    private void handleToggleKeys(MinecraftClient client) {
        if (client.player == null) return;
        // If master switch is off, ensure all toggles are disabled.
        if (!TutorialMod.CONFIG.masterEnabled) {
            if (TutorialMod.CONFIG.isToggleSneakOn) {
                TutorialMod.CONFIG.isToggleSneakOn = false;
                client.options.sneakKey.setPressed(false);
            }
            if (TutorialMod.CONFIG.isToggleSprintOn) {
                TutorialMod.CONFIG.isToggleSprintOn = false;
                client.options.sprintKey.setPressed(false);
            }
            return;
        }

        // --- Handle Toggle Sneak ---
        if (TutorialMod.CONFIG.isToggleSneakOn) {
            client.options.sneakKey.setPressed(true);
        }

        // --- Handle Toggle Sprint ---
        if (TutorialMod.CONFIG.isToggleSprintOn) {
            client.options.sprintKey.setPressed(true);
        }
    }
    private PlayerEntity getPlayerLookingAt(MinecraftClient client, double maxDistance) {
        PlayerEntity foundPlayer = null;
        double maxDot = -1.0; // cos(180)

        if (client.world == null || client.player == null) {
            return null;
        }

        Vec3d cameraPos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVector();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;

            double distance = client.player.distanceTo(player);
            if (distance > maxDistance) continue;

            Vec3d directionToPlayer = player.getEyePos().subtract(cameraPos).normalize();
            double dot = lookVec.dotProduct(directionToPlayer);

            // A larger dot product means a smaller angle.
            // We'll use a threshold of 0.99, which is about 8 degrees.
            if (dot > 0.99 && dot > maxDot) {
                maxDot = dot;
                foundPlayer = player;
            }
        }
        return foundPlayer;
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
                isPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(macro.hotkey).getCode());
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
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.getWorld() == null) return cfg.trigger; // fallback

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
            RegistryKey<World> key = player.getWorld().getRegistryKey();
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
            coords = String.format("%.3f, %.3f, %.3f", client.player.getX(), client.player.getY(), client.player.getZ());
        } else {
            coords = String.format("%d, %d, %d", (int) Math.floor(client.player.getX()), (int) Math.floor(client.player.getY()), (int) Math.floor(client.player.getZ()));
        }

        String facing = "";
        try {
            Direction d = client.player.getHorizontalFacing();
            if (d != null) {
                // Capitalize first letter
                facing = d.toString().substring(0, 1).toUpperCase() + d.toString().substring(1).toLowerCase();
            }
        } catch (Exception ignored) {}

        String result = coords + "|" + facing;
        if (TutorialMod.CONFIG.showEntityCount && client.world != null) {
            int entityCount = 0;
            for (Entity ignored : client.world.getEntities()) {
                entityCount++;
            }
            result += " E: " + entityCount;
        }

        return result;
    }
}
