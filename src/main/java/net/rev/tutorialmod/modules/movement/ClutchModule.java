package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

public class ClutchModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private boolean clutching = false;
    private int originalSlot = -1;
    private boolean blockPlaced = false;
    private boolean isSneaking = false;

    public void tick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.clutchEnabled) {
            if (clutching) reset();
            return;
        }

        var p = mc.player;

        // Reset immediately when grounded
        if (p.isOnGround()) {
            if (clutching) reset();
            return;
        }

        // Trigger based on fall distance and looking down
        if (p.fallDistance >= TutorialMod.CONFIG.clutchMinFallDistance && p.getPitch() >= TutorialMod.CONFIG.clutchActivationPitch) {
            if (!clutching) {
                clutching = true;
                originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                blockPlaced = false;
            }

            // 20 TIMES PER SECOND SPAM (Every Tick)
            BlockHitResult hit = getTargetBlock(TutorialMod.CONFIG.clutchMaxReach);
            if (hit == null) return;

            BlockState targetState = mc.world.getBlockState(hit.getBlockPos());
            boolean waterloggable = isWaterloggableWhitelist(targetState);

            if (waterloggable && !blockPlaced) {
                // PHASE A: PLACE BLOCK
                int blockSlot = findPlaceableBlock();
                if (blockSlot != -1) {
                    ((PlayerInventoryMixin) p.getInventory()).setSelectedSlot(blockSlot);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();

                    handleSneak(targetState);

                    BlockHitResult placeHit = new BlockHitResult(
                        Vec3d.ofCenter(hit.getBlockPos()),
                        Direction.UP,
                        hit.getBlockPos(),
                        false
                    );
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, placeHit);
                    p.swingHand(Hand.MAIN_HAND);

                    // Detect if block was placed to move to next stage
                    if (!mc.world.getBlockState(hit.getBlockPos().up()).isReplaceable()) {
                        blockPlaced = true;
                    }
                } else {
                    blockPlaced = true; // Fallback to water
                }
            } else {
                // PHASE B: PLACE WATER
                int waterSlot = findWaterBucket();
                if (waterSlot != -1) {
                    ((PlayerInventoryMixin) p.getInventory()).setSelectedSlot(waterSlot);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();

                    handleSneak(targetState);

                    BlockHitResult placeHit = new BlockHitResult(
                        Vec3d.ofCenter(hit.getBlockPos()),
                        Direction.UP,
                        hit.getBlockPos(),
                        false
                    );
                    mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, placeHit);
                    p.swingHand(Hand.MAIN_HAND);
                }
            }
        }
    }

    private void handleSneak(BlockState state) {
        if (isInteractable(state)) {
            mc.options.sneakKey.setPressed(true);
            isSneaking = true;
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
        clutching = false;
        blockPlaced = false;
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
