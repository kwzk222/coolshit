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
import net.minecraft.registry.Registries;

public class ClutchModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private enum ClutchState {
        IDLE,
        FALLING,
        AIMING,
        SWITCH_TO_BLOCK,
        PLACE_BLOCK,
        SWITCH_TO_WATER,
        PLACE_WATER,
        COOLDOWN
    }

    private ClutchState state = ClutchState.IDLE;
    private int tickDelay = 0;
    private int originalSlot = -1;
    private boolean isSneaking = false;
    private BlockPos lastTargetPos = null;

    public void tick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.clutchEnabled) {
            if (state != ClutchState.IDLE) reset();
            return;
        }

        if (tickDelay > 0) {
            tickDelay--;
            return;
        }

        var p = mc.player;

        switch (state) {
            case IDLE -> {
                if (!p.isOnGround() && p.fallDistance >= TutorialMod.CONFIG.clutchMinFallDistance && p.getVelocity().y < -0.1) {
                    state = ClutchState.FALLING;
                }
            }

            case FALLING -> {
                if (p.isOnGround()) {
                    state = ClutchState.IDLE;
                    return;
                }

                // Predictive check
                double predictedY = p.getY() + p.getVelocity().y * 2.0;
                BlockHitResult ray = getTargetBlock(4.5);

                if (p.getPitch() >= TutorialMod.CONFIG.clutchActivationPitch || (ray != null && predictedY <= ray.getBlockPos().getY() + 3.2)) {
                    state = ClutchState.AIMING;
                }
            }

            case AIMING -> {
                if (p.isOnGround()) {
                    reset();
                    return;
                }

                BlockHitResult hit = getTargetBlock(4.5);
                if (hit != null) {
                    lastTargetPos = hit.getBlockPos();
                    BlockState blockState = mc.world.getBlockState(lastTargetPos);

                    if (originalSlot == -1) {
                        originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                    }

                    if (isWaterloggableWhitelist(blockState)) {
                        state = ClutchState.SWITCH_TO_BLOCK;
                    } else {
                        state = ClutchState.SWITCH_TO_WATER;
                    }
                }
            }

            case SWITCH_TO_BLOCK -> {
                int blockSlot = findPlaceableBlock();
                if (blockSlot != -1) {
                    ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(blockSlot);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
                    tickDelay = 1;
                    state = ClutchState.PLACE_BLOCK;
                } else {
                    state = ClutchState.SWITCH_TO_WATER;
                }
            }

            case PLACE_BLOCK -> {
                BlockHitResult hit = getTargetBlock(4.5);
                BlockPos targetPos = (hit != null) ? hit.getBlockPos() : lastTargetPos;

                if (targetPos != null) {
                    handleSneak(mc.world.getBlockState(targetPos));

                    // Interaction with the top face of the block
                    BlockHitResult placementHit = new BlockHitResult(
                        Vec3d.ofCenter(targetPos).add(0, 0.5, 0),
                        Direction.UP,
                        targetPos,
                        false
                    );
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, placementHit);
                    p.swingHand(Hand.MAIN_HAND);

                    lastTargetPos = targetPos.up();
                    state = ClutchState.SWITCH_TO_WATER;
                    tickDelay = 1;
                } else {
                    reset();
                }
            }

            case SWITCH_TO_WATER -> {
                int waterSlot = findWaterBucket();
                if (waterSlot != -1) {
                    ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(waterSlot);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
                    tickDelay = 1;
                    state = ClutchState.PLACE_WATER;
                } else {
                    reset();
                }
            }

            case PLACE_WATER -> {
                BlockHitResult hit = getTargetBlock(4.5);
                BlockPos targetPos = (hit != null) ? hit.getBlockPos() : lastTargetPos;

                if (targetPos != null) {
                    handleSneak(mc.world.getBlockState(targetPos));

                    BlockHitResult placementHit = new BlockHitResult(
                        Vec3d.ofCenter(targetPos).add(0, 0.5, 0),
                        Direction.UP,
                        targetPos,
                        false
                    );
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, placementHit);
                    p.swingHand(Hand.MAIN_HAND);

                    state = ClutchState.COOLDOWN;
                    tickDelay = 1;
                } else {
                    reset();
                }
            }

            case COOLDOWN -> {
                reset();
            }
        }
    }

    private void handleSneak(BlockState state) {
        if (isInteractable(state)) {
            mc.options.sneakKey.setPressed(true);
            isSneaking = true;
        } else {
            mc.options.sneakKey.setPressed(isManualSneakPressed());
            isSneaking = false;
        }
    }

    private void reset() {
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
        tickDelay = 0;
        lastTargetPos = null;
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
