package net.rev.tutorialmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.rev.tutorialmod.event.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import net.minecraft.text.Text;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.rev.tutorialmod.modules.TriggerBot;

public class TutorialModClient implements ClientModInitializer {

    private static TutorialModClient instance;
    private TriggerBot triggerBot;

    // --- State Machines ---
    private enum SwapAction { NONE, SWITCH_BACK, SWITCH_BACK_ATTACK_MACE }

    private enum PlacementAction {
        NONE,
        PLACE_TNT_MINECART,
        AWAITING_LAVA_PLACEMENT,
        AWAITING_FIRE_PLACEMENT,
        SWITCH_TO_CROSSBOW,
        SWITCH_TO_BOW
    }

    // --- State Fields ---
    private int swapCooldown = -1;
    private int originalHotbarSlot = -1;
    private SwapAction nextAction = SwapAction.NONE;
    private Entity targetEntity = null;

    private int placementCooldown = -1;
    private PlacementAction nextPlacementAction = PlacementAction.NONE;
    private BlockPos railPos = null;
    private int utilitySlot = -1; // Can be lava bucket or flint and steel
    private int crossbowSlot = -1;
    private int actionTimeout = -1;

    public static long lastBowShotTick = -1;
    public static int awaitingRailConfirmationCooldown = -1;
    private static KeyBinding masterToggleKeybind;
    private static KeyBinding teammateKeybind;

    @Override
    public void onInitializeClient() {
        instance = this;
        masterToggleKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tutorialmod.master_toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.tutorialmod"));
        teammateKeybind = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tutorialmod.teammate_toggle", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, "key.categories.tutorialmod"));
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        AttackEntityCallback.EVENT.register(this::onAttackEntity);
        new CommandManager().registerCommands();
        triggerBot = new TriggerBot();
    }

    private void onClientTick(MinecraftClient client) {
        handleKeybinds(client);
        if (triggerBot != null) {
            triggerBot.onTick(client);
        }
        if (!TutorialMod.CONFIG.masterEnabled) return;
        handleTotemSwap(client);
        handleCombatSwap(client);
        handlePlacementSequence(client);
        handleConfirmationCooldowns(client);
    }

    // --- Tick Logic Handlers ---

    private void handleKeybinds(MinecraftClient client) {
        while (masterToggleKeybind.wasPressed()) {
            TutorialMod.CONFIG.masterEnabled = !TutorialMod.CONFIG.masterEnabled;
            TutorialMod.CONFIG.save();
            if (client.player != null) {
                client.player.sendMessage(Text.of("TutorialMod Master Switch: " + (TutorialMod.CONFIG.masterEnabled ? "ON" : "OFF")), false);
            }
        }
        while (teammateKeybind.wasPressed()) {
            handleTeammateKeybind(client);
        }
    }

    private void handleTeammateKeybind(MinecraftClient client) {
        if (client.crosshairTarget != null && client.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity target = ((net.minecraft.util.hit.EntityHitResult) client.crosshairTarget).getEntity();
            if (target instanceof PlayerEntity) {
                String name = target.getName().getString();
                if (TutorialMod.CONFIG.teamManager.isTeammate(name)) {
                    TutorialMod.CONFIG.teamManager.removeTeammate(name);
                    client.player.sendMessage(Text.of("Removed " + name + " from your teammates list."), false);
                } else {
                    TutorialMod.CONFIG.teamManager.addTeammate(name);
                    client.player.sendMessage(Text.of("Added " + name + " to your teammates list."), false);
                }
            }
        }
    }

    private void handleTotemSwap(MinecraftClient client) {
        if (!TutorialMod.CONFIG.totemSwapEnabled) return;
        if (client.player == null || client.currentScreen == null || client.interactionManager == null || client.player.isCreative()) return;
        if (client.currentScreen instanceof HandledScreen<?> screen) {
            if (client.player.getOffHandStack().getItem() == Items.TOTEM_OF_UNDYING) return;
            double scaledMouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
            double scaledMouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();
            int guiLeft = (client.getWindow().getScaledWidth() - 176) / 2;
            int guiTop = (client.getWindow().getScaledHeight() - 166) / 2;
            for (Slot slot : screen.getScreenHandler().slots) {
                if (slot.getStack().getItem() == Items.TOTEM_OF_UNDYING) {
                    int slotX = guiLeft + slot.x;
                    int slotY = guiTop + slot.y;
                    if (scaledMouseX >= slotX && scaledMouseX < slotX + 16 && scaledMouseY >= slotY && scaledMouseY < slotY + 16) {
                        client.interactionManager.clickSlot(screen.getScreenHandler().syncId, slot.id, 40, net.minecraft.screen.slot.SlotActionType.SWAP, client.player);
                        break;
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
                case SWITCH_BACK_ATTACK_MACE:
                    inventory.setSelectedSlot(originalHotbarSlot);
                    if (client.interactionManager != null && targetEntity != null && targetEntity.isAlive()) {
                        client.interactionManager.attackEntity(client.player, targetEntity);
                        int maceSlot = findMaceInHotbar(client.player);
                        if (maceSlot != -1) {
                            inventory.setSelectedSlot(maceSlot);
                            swapCooldown = TutorialMod.CONFIG.postComboAxeSwapDelay;
                            nextAction = SwapAction.SWITCH_BACK;
                        }
                    }
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

    // --- Event Handlers ---

    private ActionResult onAttackEntity(PlayerEntity player, Entity target) {
        if (!TutorialMod.CONFIG.masterEnabled || swapCooldown != -1) return ActionResult.PASS;
        if (target instanceof PlayerEntity) {
            PlayerEntity attackedPlayer = (PlayerEntity) target;
            boolean isShielding = attackedPlayer.isUsingItem() && attackedPlayer.getActiveItem().getItem() == Items.SHIELD;

            // Facing Check
            Vec3d selfPos = player.getPos();
            Vec3d targetPos = attackedPlayer.getPos();
            Vec3d targetLookVec = attackedPlayer.getRotationVector();
            Vec3d vecToSelf = selfPos.subtract(targetPos).normalize();
            boolean isFacing = vecToSelf.dotProduct(targetLookVec) > 0;

            boolean hasArmor = isArmored(attackedPlayer);
            PlayerInventoryMixin inventory = (PlayerInventoryMixin) player.getInventory();
            if (isShielding && isFacing && TutorialMod.CONFIG.axeSwapEnabled) {
                int axeSlot = findAxeInHotbar(player);
                if (axeSlot != -1 && inventory.getSelectedSlot() != axeSlot) {
                    originalHotbarSlot = inventory.getSelectedSlot();
                    inventory.setSelectedSlot(axeSlot);
                    targetEntity = target;
                    int maceSlot = findMaceInHotbar(player);
                    if (hasArmor && maceSlot != -1 && TutorialMod.CONFIG.maceSwapEnabled && player.fallDistance > TutorialMod.CONFIG.minFallDistance) {
                        swapCooldown = TutorialMod.CONFIG.comboSwapDelay;
                        nextAction = SwapAction.SWITCH_BACK_ATTACK_MACE;
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

    // --- Helper Methods ---

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
                    this.actionTimeout = 60; // 3 seconds
                    this.placementCooldown = 1;
                    this.nextPlacementAction = PlacementAction.AWAITING_LAVA_PLACEMENT;
                    return;
                }
                int flintSlot = findFlintAndSteelInHotbar(client.player);
                if (flintSlot != -1) {
                    this.utilitySlot = flintSlot;
                    this.crossbowSlot = crossSlot;
                    inventory.setSelectedSlot(flintSlot);
                    this.actionTimeout = 60; // 3 seconds
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

    public static void recordBowUsage() {
        if (instance != null) {
            instance.lastBowShotTick = MinecraftClient.getInstance().world.getTime();
        }
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
}
