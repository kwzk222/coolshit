package net.rev.tutorialmod.modules;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;

/**
 * Handles the Minecart Tech feature, which automates the placement of TNT minecarts and subsequent actions.
 */
public class MinecartTech {
    // Enum to manage the state of the placement sequence
    private enum PlacementAction { NONE, PLACE_TNT_MINECART, AWAITING_LAVA_PLACEMENT, AWAITING_FIRE_PLACEMENT, SWITCH_TO_CROSSBOW, SWITCH_TO_BOW }
    private int placementCooldown = -1;
    private PlacementAction nextPlacementAction = PlacementAction.NONE;
    private BlockPos railPos = null;
    private int utilitySlot = -1;
    private int crossbowSlot = -1;
    private int actionTimeout = -1;
    public static long lastBowShotTick = -1;
    public static int awaitingRailConfirmationCooldown = -1;
    private static int clickCooldown = -1;

    /**
     * Called every client tick to handle the minecart tech logic.
     * @param client The Minecraft client instance.
     */
    public void onClientTick(MinecraftClient client) {
        handlePlacementSequence(client);
        handleConfirmationCooldowns(client);
        handlePlacementClick(client);
    }

    private void handlePlacementClick(MinecraftClient client) {
        if (clickCooldown > 0) {
            clickCooldown--;
        } else if (clickCooldown == 0) {
            client.options.useKey.setPressed(false);
            clickCooldown = -1;
        }
    }

    private void handleConfirmationCooldowns(MinecraftClient client) {
        if (awaitingRailConfirmationCooldown > 0) {
            awaitingRailConfirmationCooldown--;
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

    public static void setAwaitingRailConfirmation() {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.tntMinecartPlacementEnabled) return;
        awaitingRailConfirmationCooldown = 2;
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

    private int findTntMinecartInHotbar(net.minecraft.entity.player.PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.TNT_MINECART) return i;
        }
        return -1;
    }

    private int findLavaBucketInHotbar(net.minecraft.entity.player.PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.LAVA_BUCKET) return i;
        }
        return -1;
    }

    private int findLoadedCrossbowInHotbar(net.minecraft.entity.player.PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.CROSSBOW && CrossbowItem.isCharged(stack)) {
                return i;
            }
        }
        return -1;
    }

    private int findBowInHotbar(net.minecraft.entity.player.PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.BOW) return i;
        }
        return -1;
    }

    private int findFlintAndSteelInHotbar(net.minecraft.entity.player.PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.FLINT_AND_STEEL) return i;
        }
        return -1;
    }

    public static void confirmLavaPlacement(BlockPos pos, BlockState state, MinecartTech instance) {
        if (instance != null && instance.nextPlacementAction == PlacementAction.AWAITING_LAVA_PLACEMENT && state.getBlock() == net.minecraft.block.Blocks.LAVA) {
            instance.placementCooldown = 1;
            instance.nextPlacementAction = PlacementAction.SWITCH_TO_CROSSBOW;
            instance.actionTimeout = -1;
        }
    }

    public static void confirmFirePlacement(BlockPos pos, BlockState state, MinecartTech instance) {
        if (instance != null && instance.nextPlacementAction == PlacementAction.AWAITING_FIRE_PLACEMENT && state.getBlock() == net.minecraft.block.Blocks.FIRE) {
            instance.placementCooldown = 1;
            instance.nextPlacementAction = PlacementAction.SWITCH_TO_CROSSBOW;
            instance.actionTimeout = -1;
        }
    }
}
