package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class ClutchModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private enum ClutchState {
        IDLE,
        FALLING,
        PLACING_BLOCK,
        PLACING_WATER,
        LANDED,
        RECOVERING,
        COOLDOWN
    }

    private ClutchState state = ClutchState.IDLE;
    private int originalSlot = -1;
    private BlockPos targetPos = null;
    private int tickCounter = 0;
    private boolean isSneaking = false;

    private static final int RECOVERY_DELAY = 20; // Ticks after landing to wait before picking up water
    private static final int MAX_RECOVERY_TIME = 60; // Max ticks to try recovering

    public void tick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.clutchEnabled) {
            if (state != ClutchState.IDLE) reset();
            return;
        }

        var p = mc.player;

        switch (state) {
            case IDLE -> {
                if (!p.isOnGround() && p.fallDistance >= TutorialMod.CONFIG.clutchMinFallDistance && p.getPitch() >= TutorialMod.CONFIG.clutchActivationPitch) {
                    state = ClutchState.FALLING;
                    originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                }
            }

            case FALLING -> {
                if (p.isOnGround()) {
                    reset();
                    return;
                }

                BlockHitResult hit = getTargetBlock(TutorialMod.CONFIG.clutchMaxReach);
                if (hit != null) {
                    targetPos = hit.getBlockPos();
                    BlockState targetState = mc.world.getBlockState(targetPos);

                    if (isWaterloggableWhitelist(targetState)) {
                        state = ClutchState.PLACING_BLOCK;
                    } else {
                        state = ClutchState.PLACING_WATER;
                    }
                }
            }

            case PLACING_BLOCK -> {
                if (p.isOnGround()) {
                    reset();
                    return;
                }

                int blockSlot = findPlaceableBlock();
                if (blockSlot == -1) {
                    state = ClutchState.PLACING_WATER; // Fallback to water if no blocks
                    return;
                }

                setSlot(blockSlot);
                updateSneakState(mc.world.getBlockState(targetPos));
                mc.options.useKey.setPressed(true);

                // Detect if block was placed (check space above target)
                if (!mc.world.getBlockState(targetPos.up()).isReplaceable()) {
                    mc.options.useKey.setPressed(false);
                    state = ClutchState.PLACING_WATER;
                }
            }

            case PLACING_WATER -> {
                if (p.isOnGround()) {
                    if (waterPlacedBelow(p)) {
                        mc.options.useKey.setPressed(false);
                        tickCounter = 0;
                        state = ClutchState.LANDED;
                    } else {
                        reset();
                    }
                    return;
                }

                int waterSlot = findWaterBucket();
                if (waterSlot == -1) {
                    // Landed check will catch it next tick
                    return;
                }

                setSlot(waterSlot);
                updateSneakState(mc.world.getBlockState(targetPos));
                mc.options.useKey.setPressed(true);
            }

            case LANDED -> {
                tickCounter++;
                if (tickCounter >= RECOVERY_DELAY) {
                    tickCounter = 0;
                    state = ClutchState.RECOVERING;
                }
            }

            case RECOVERING -> {
                tickCounter++;
                if (tickCounter >= MAX_RECOVERY_TIME) {
                    reset();
                    return;
                }

                int bucketSlot = findEmptyBucket();
                if (bucketSlot == -1) {
                    // Check if current slot is the bucket (even if it's the water bucket slot)
                    for (int i = 0; i < 9; i++) {
                        if (p.getInventory().getStack(i).isOf(Items.BUCKET)) {
                            bucketSlot = i;
                            break;
                        }
                    }
                }

                if (bucketSlot != -1) {
                    setSlot(bucketSlot);
                    mc.options.useKey.setPressed(true);

                    // Success when bucket is full again (water bucket)
                    if (p.getInventory().getStack(bucketSlot).isOf(Items.WATER_BUCKET)) {
                        reset();
                    }
                } else {
                    reset();
                }
            }

            case COOLDOWN -> reset();
        }
    }

    private void setSlot(int slot) {
        if (mc.player != null && ((PlayerInventoryMixin) mc.player.getInventory()).getSelectedSlot() != slot) {
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(slot);
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
        }
    }

    private void updateSneakState(BlockState targetState) {
        if (isInteractable(targetState)) {
            mc.options.sneakKey.setPressed(true);
            isSneaking = true;
        } else if (isSneaking) {
            mc.options.sneakKey.setPressed(isManualSneakPressed());
            isSneaking = false;
        }
    }

    private boolean waterPlacedBelow(net.minecraft.client.network.ClientPlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        return mc.world.getFluidState(pos).isOf(Fluids.WATER) ||
               mc.world.getFluidState(pos.down()).isOf(Fluids.WATER) ||
               mc.world.getFluidState(pos.up()).isOf(Fluids.WATER);
    }

    private void reset() {
        mc.options.useKey.setPressed(isManualUsePressed());
        if (isSneaking) {
            mc.options.sneakKey.setPressed(isManualSneakPressed());
            isSneaking = false;
        }
        if (originalSlot != -1 && mc.player != null) {
            setSlot(originalSlot);
            originalSlot = -1;
        }
        state = ClutchState.IDLE;
        tickCounter = 0;
        targetPos = null;
    }

    private BlockHitResult getTargetBlock(double reach) {
        HitResult hit = mc.player.raycast(reach, 1.0f, false);
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            return (BlockHitResult) hit;
        }
        return null;
    }

    private boolean isWaterloggableWhitelist(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock) return true;
        if (block instanceof StairsBlock) return true;
        if (block instanceof SlabBlock && state.contains(SlabBlock.TYPE)) {
            return state.get(SlabBlock.TYPE) != SlabType.BOTTOM;
        }
        if (block instanceof TrapdoorBlock && state.contains(TrapdoorBlock.HALF) && state.contains(TrapdoorBlock.OPEN)) {
            return state.get(TrapdoorBlock.HALF) == BlockHalf.TOP && !state.get(TrapdoorBlock.OPEN);
        }

        return block == Blocks.CAULDRON
                || block == Blocks.BIG_DRIPLEAF
                || block == Blocks.COPPER_GRATE
                || block == Blocks.DECORATED_POT
                || block == Blocks.MANGROVE_ROOTS;
    }

    private boolean isInteractable(BlockState state) {
        Block block = state.getBlock();
        return block instanceof ChestBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof CraftingTableBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock
                || block instanceof DropperBlock
                || block instanceof AnvilBlock
                || block instanceof CartographyTableBlock
                || block instanceof FletchingTableBlock
                || block instanceof SmithingTableBlock
                || block instanceof LoomBlock
                || block instanceof StonecutterBlock
                || block instanceof GrindstoneBlock
                || block instanceof EnchantingTableBlock
                || block instanceof LecternBlock
                || block instanceof BrewingStandBlock
                || block instanceof NoteBlock
                || block instanceof JukeboxBlock
                || block instanceof TrapdoorBlock
                || block instanceof DoorBlock
                || block instanceof FenceGateBlock
                || block instanceof LeverBlock
                || block instanceof ButtonBlock;
    }

    private int findWaterBucket() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.WATER_BUCKET))
                return i;
        }
        return -1;
    }

    private int findEmptyBucket() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.BUCKET))
                return i;
        }
        return -1;
    }

    private int findPlaceableBlock() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem)
                return i;
        }
        return -1;
    }

    private boolean isManualSneakPressed() {
        try {
            return net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                    net.minecraft.client.util.InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isManualUsePressed() {
        try {
            return net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                    net.minecraft.client.util.InputUtil.fromTranslationKey(mc.options.useKey.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
    }
}
