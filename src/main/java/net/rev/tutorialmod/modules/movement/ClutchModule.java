package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
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
        PLACING,
        COOLDOWN
    }

    private ClutchState state = ClutchState.IDLE;
    private BlockPos targetPos;
    private BlockHitResult targetHit;
    private boolean isWhitelisted;
    private boolean blockPlaced;
    private boolean isSneaking;
    private int originalSlot = -1;

    public void tick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.clutchEnabled) {
            reset();
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
                if (p.getPitch() < TutorialMod.CONFIG.clutchActivationPitch - 5.0f) {
                    state = ClutchState.FALLING;
                    return;
                }

                BlockHitResult hit = getTargetBlock();
                if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                    targetHit = hit;
                    targetPos = hit.getBlockPos();
                    BlockState blockState = mc.world.getBlockState(targetPos);
                    isWhitelisted = isWaterloggableWhitelist(blockState);

                    if (isInteractable(blockState)) {
                        mc.options.sneakKey.setPressed(true);
                        isSneaking = true;
                    }

                    originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                    blockPlaced = false;
                    state = ClutchState.PLACING;
                }
            }

            case PLACING -> {
                if (isWhitelisted) {
                    if (!blockPlaced) {
                        int blockSlot = findPlaceableBlock();
                        if (blockSlot != -1) {
                            ((PlayerInventoryMixin) p.getInventory()).setSelectedSlot(blockSlot);
                            mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, targetHit);
                            blockPlaced = true;
                            // Update targetHit for water placement on top of the new block
                            targetHit = new BlockHitResult(targetHit.getPos().add(0, 1, 0), Direction.UP, targetPos.offset(targetHit.getSide()), false);
                        } else {
                            // Fallback to water if no block found
                            isWhitelisted = false;
                        }
                    } else {
                        placeWater();
                        state = ClutchState.COOLDOWN;
                    }
                } else {
                    placeWater();
                    state = ClutchState.COOLDOWN;
                }
            }

            case COOLDOWN -> {
                reset();
            }
        }
    }

    private void placeWater() {
        int waterSlot = findWaterBucket();
        if (waterSlot != -1) {
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(waterSlot);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, targetHit);
        }
    }

    private void reset() {
        if (isSneaking) {
            mc.options.sneakKey.setPressed(isManualSneakPressed());
            isSneaking = false;
        }
        if (originalSlot != -1 && mc.player != null) {
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(originalSlot);
            originalSlot = -1;
        }
        state = ClutchState.IDLE;
        targetPos = null;
        targetHit = null;
        blockPlaced = false;
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
