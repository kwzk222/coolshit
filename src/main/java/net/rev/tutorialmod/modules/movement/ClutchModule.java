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
                if (p.getPitch() >= TutorialMod.CONFIG.clutchActivationPitch) {
                    state = ClutchState.AIMING;
                }
            }

            case AIMING -> {
                if (p.isOnGround()) {
                    reset();
                    return;
                }
                // Hysteresis: stay in aiming unless pitch drops significantly
                if (p.getPitch() < TutorialMod.CONFIG.clutchActivationPitch - 5.0f) {
                    state = ClutchState.FALLING;
                    return;
                }

                BlockHitResult hit = getTargetBlock();
                if (hit != null) {
                    BlockState blockState = mc.world.getBlockState(hit.getBlockPos());

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
                    tickDelay = 2;
                    state = ClutchState.PLACE_BLOCK;
                } else {
                    // No block? Try water directly as fallback
                    state = ClutchState.SWITCH_TO_WATER;
                }
            }

            case PLACE_BLOCK -> {
                BlockHitResult hit = getTargetBlock();
                if (hit != null) {
                    BlockState blockState = mc.world.getBlockState(hit.getBlockPos());
                    handleSneak(blockState);

                    // Force Direction.UP for clutch placement
                    BlockHitResult placementHit = new BlockHitResult(hit.getPos(), Direction.UP, hit.getBlockPos(), false);
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, placementHit);

                    state = ClutchState.SWITCH_TO_WATER;
                    tickDelay = 1; // Short delay before switching to water
                } else {
                    reset(); // Lost target
                }
            }

            case SWITCH_TO_WATER -> {
                int waterSlot = findWaterBucket();
                if (waterSlot != -1) {
                    ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(waterSlot);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
                    tickDelay = 2;
                    state = ClutchState.PLACE_WATER;
                } else {
                    reset(); // No water
                }
            }

            case PLACE_WATER -> {
                BlockHitResult hit = getTargetBlock();
                if (hit != null) {
                    BlockState blockState = mc.world.getBlockState(hit.getBlockPos());
                    handleSneak(blockState);

                    BlockHitResult placementHit = new BlockHitResult(hit.getPos(), Direction.UP, hit.getBlockPos(), false);
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, placementHit);

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
    }

    private BlockHitResult getTargetBlock() {
        HitResult hit = mc.player.raycast(TutorialMod.CONFIG.clutchMaxReach, 1.0f, false);
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

        if (block == Blocks.CAULDRON) return true;
        if (block == Blocks.BIG_DRIPLEAF) return true;
        if (block == Blocks.COPPER_GRATE) return true;
        if (block == Blocks.DECORATED_POT) return true;
        if (block == Blocks.MANGROVE_ROOTS) return true;

        String id = Registries.BLOCK.getId(block).getPath();
        return id.contains("copper_grate");
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
