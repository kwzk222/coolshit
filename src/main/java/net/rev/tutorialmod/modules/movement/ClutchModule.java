package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class ClutchModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private enum ClutchState {
        IDLE,
        FALLING,
        AIMING,
        SWITCHING_TO_BLOCK,
        HOLDING_USE_BLOCK,
        SWITCHING_TO_WATER,
        HOLDING_USE_WATER,
        RESTORE_SLOT,
        COOLDOWN
    }

    private ClutchState state = ClutchState.IDLE;
    private int stateTimer = 0;
    private int originalSlot = -1;
    private boolean isSneaking = false;
    private int totalClutchTimer = 0;

    // Timing constants
    private static final int USE_HOLD_TICKS = 6;
    private static final int MAX_CLUTCH_TICKS = 60;

    public void tick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.clutchEnabled) {
            if (state != ClutchState.IDLE) reset();
            return;
        }

        var p = mc.player;

        if (state != ClutchState.IDLE) {
            totalClutchTimer++;
            if (totalClutchTimer > MAX_CLUTCH_TICKS || p.isOnGround()) {
                state = ClutchState.RESTORE_SLOT;
            }
        }

        switch (state) {
            case IDLE -> {
                if (!p.isOnGround() && p.fallDistance >= TutorialMod.CONFIG.clutchMinFallDistance && p.getVelocity().y < -0.1) {
                    state = ClutchState.FALLING;
                    totalClutchTimer = 0;
                }
            }

            case FALLING -> {
                // Abort if falling too fast/too late
                if (p.getVelocity().y < -3.5) { // Roughly 20+ block fall speed
                    // reset? Or just wait?
                }

                // Predictive check
                BlockHitResult ray = getTargetBlock(4.5);
                boolean inRange = false;
                if (ray != null) {
                    double dist = p.getY() - ray.getBlockPos().getY();
                    if (dist <= 4.0 && dist >= 1.0) {
                        inRange = true;
                    }
                }

                if (p.getPitch() >= TutorialMod.CONFIG.clutchActivationPitch || inRange) {
                    state = ClutchState.AIMING;
                }
            }

            case AIMING -> {
                BlockHitResult hit = getTargetBlock(4.5);
                if (hit != null) {
                    BlockState blockState = mc.world.getBlockState(hit.getBlockPos());

                    if (originalSlot == -1) {
                        originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                    }

                    if (isWaterloggableWhitelist(blockState)) {
                        state = ClutchState.SWITCHING_TO_BLOCK;
                    } else {
                        state = ClutchState.SWITCHING_TO_WATER;
                    }
                }
            }

            case SWITCHING_TO_BLOCK -> {
                int blockSlot = findPlaceableBlock();
                if (blockSlot != -1) {
                    ((PlayerInventoryMixin) p.getInventory()).setSelectedSlot(blockSlot);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
                    state = ClutchState.HOLDING_USE_BLOCK;
                    stateTimer = USE_HOLD_TICKS;
                } else {
                    state = ClutchState.SWITCHING_TO_WATER;
                }
            }

            case HOLDING_USE_BLOCK -> {
                mc.options.useKey.setPressed(true);
                updateSneakState();
                stateTimer--;
                if (stateTimer <= 0) {
                    mc.options.useKey.setPressed(false);
                    state = ClutchState.SWITCHING_TO_WATER;
                }
            }

            case SWITCHING_TO_WATER -> {
                int waterSlot = findWaterBucket();
                if (waterSlot != -1) {
                    ((PlayerInventoryMixin) p.getInventory()).setSelectedSlot(waterSlot);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
                    state = ClutchState.HOLDING_USE_WATER;
                    stateTimer = USE_HOLD_TICKS;
                } else {
                    state = ClutchState.RESTORE_SLOT;
                }
            }

            case HOLDING_USE_WATER -> {
                mc.options.useKey.setPressed(true);
                updateSneakState();
                stateTimer--;
                if (stateTimer <= 0) {
                    mc.options.useKey.setPressed(false);
                    state = ClutchState.RESTORE_SLOT;
                }
            }

            case RESTORE_SLOT -> {
                mc.options.useKey.setPressed(false);
                updateSneakState(); // Ensure sneak is updated/released

                if (originalSlot != -1) {
                    ((PlayerInventoryMixin) p.getInventory()).setSelectedSlot(originalSlot);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
                    originalSlot = -1;
                }
                state = ClutchState.COOLDOWN;
                stateTimer = 5;
            }

            case COOLDOWN -> {
                stateTimer--;
                if (stateTimer <= 0) {
                    reset();
                }
            }
        }
    }

    private void updateSneakState() {
        BlockHitResult hit = getTargetBlock(4.5);
        if (hit != null) {
            BlockState blockState = mc.world.getBlockState(hit.getBlockPos());
            if (isInteractable(blockState)) {
                mc.options.sneakKey.setPressed(true);
                isSneaking = true;
                return;
            }
        }

        // If not looking at interactable, or no hit, revert to manual state
        mc.options.sneakKey.setPressed(isManualSneakPressed());
        isSneaking = false;
    }

    private void reset() {
        mc.options.useKey.setPressed(false);
        mc.options.sneakKey.setPressed(isManualSneakPressed());
        isSneaking = false;

        if (originalSlot != -1 && mc.player != null) {
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(originalSlot);
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
            originalSlot = -1;
        }
        state = ClutchState.IDLE;
        stateTimer = 0;
        totalClutchTimer = 0;
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
            return InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                    InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
    }
}
