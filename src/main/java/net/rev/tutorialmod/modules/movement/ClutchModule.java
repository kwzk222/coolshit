package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class ClutchModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private enum ClutchState {
        IDLE,
        ARMED_BLOCK,
        EXECUTING_BLOCK,
        ARMED_WATER,
        EXECUTING_WATER
    }

    private ClutchState state = ClutchState.IDLE;
    private int originalSlot = -1;
    private int execTicks = 0;
    private boolean usingItem = false;
    private boolean isSneaking = false;

    // Constants
    private static final double ARM_VELOCITY = -0.08;
    private static final double EXEC_VELOCITY = -0.9;
    private static final double ABORT_VELOCITY = -2.1;
    private static final int MAX_EXEC_TICKS = 10;

    public void tick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.clutchEnabled) {
            if (state != ClutchState.IDLE) reset();
            return;
        }

        var p = mc.player;

        switch (state) {
            case IDLE -> {
                // Respect all configurations for triggering
                if (!p.isOnGround() && p.getVelocity().y < ARM_VELOCITY
                        && p.getPitch() >= TutorialMod.CONFIG.clutchActivationPitch
                        && p.fallDistance >= TutorialMod.CONFIG.clutchMinFallDistance) {

                    BlockHitResult ray = getTargetBlock(TutorialMod.CONFIG.clutchMaxReach + 2.0);
                    if (ray != null) {
                        originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();

                        BlockState targetState = mc.world.getBlockState(ray.getBlockPos());
                        if (isWaterloggableWhitelist(targetState)) {
                            int blockSlot = findPlaceableBlock();
                            if (blockSlot != -1) {
                                ((PlayerInventoryMixin) p.getInventory()).setSelectedSlot(blockSlot);
                                ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
                                state = ClutchState.ARMED_BLOCK;
                            } else {
                                armWater(p);
                            }
                        } else {
                            armWater(p);
                        }
                    }
                }
            }

            case ARMED_BLOCK -> {
                if (p.isOnGround()) {
                    reset();
                    return;
                }
                if (p.getVelocity().y < EXEC_VELOCITY) {
                    execTicks = 0;
                    state = ClutchState.EXECUTING_BLOCK;
                }
            }

            case EXECUTING_BLOCK -> {
                if (p.isOnGround() || p.getVelocity().y < ABORT_VELOCITY || execTicks >= 4) { // Blocks don't need 10 ticks
                    stopUsing();
                    armWater(p);
                    return;
                }

                startUsing();
                updateSneakState();
                execTicks++;

                // Check if block was placed (crude check: solid block above target)
                BlockHitResult ray = getTargetBlock(TutorialMod.CONFIG.clutchMaxReach);
                if (ray != null && mc.world.getBlockState(ray.getBlockPos().up()).isSolid()) {
                    stopUsing();
                    armWater(p);
                }
            }

            case ARMED_WATER -> {
                if (p.isOnGround()) {
                    reset();
                    return;
                }
                if (p.getVelocity().y < EXEC_VELOCITY) {
                    execTicks = 0;
                    state = ClutchState.EXECUTING_WATER;
                }
            }

            case EXECUTING_WATER -> {
                if (p.isOnGround() || p.getVelocity().y < ABORT_VELOCITY || execTicks >= MAX_EXEC_TICKS) {
                    reset();
                    return;
                }

                startUsing();
                updateSneakState();
                execTicks++;

                if (waterPlacedBelow(p)) {
                    reset();
                }
            }
        }
    }

    private void armWater(ClientPlayerEntity p) {
        int waterSlot = findWaterBucket();
        if (waterSlot != -1) {
            ((PlayerInventoryMixin) p.getInventory()).setSelectedSlot(waterSlot);
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
            state = ClutchState.ARMED_WATER;
        } else {
            reset();
        }
    }

    private void startUsing() {
        if (!usingItem) {
            mc.options.useKey.setPressed(true);
            usingItem = true;
        }
    }

    private void stopUsing() {
        mc.options.useKey.setPressed(false);
        usingItem = false;
    }

    private boolean waterPlacedBelow(ClientPlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        // Check current block and 2 blocks below for water
        for (int y = 0; y >= -2; y--) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (p.getWorld().getFluidState(pos.add(x, y, z)).isOf(Fluids.WATER)) return true;
                }
            }
        }
        return false;
    }

    private void updateSneakState() {
        BlockHitResult hit = getTargetBlock(TutorialMod.CONFIG.clutchMaxReach);
        if (hit != null) {
            BlockState blockState = mc.world.getBlockState(hit.getBlockPos());
            if (isInteractable(blockState)) {
                mc.options.sneakKey.setPressed(true);
                isSneaking = true;
                return;
            }
        }

        if (isSneaking) {
            mc.options.sneakKey.setPressed(isManualSneakPressed());
            isSneaking = false;
        }
    }

    private void reset() {
        stopUsing();
        if (isSneaking) {
            mc.options.sneakKey.setPressed(isManualSneakPressed());
            isSneaking = false;
        }

        if (originalSlot != -1 && mc.player != null) {
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(originalSlot);
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
            originalSlot = -1;
        }
        state = ClutchState.IDLE;
        execTicks = 0;
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
            return state.get(SlabBlock.TYPE) == SlabType.TOP;
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
}
