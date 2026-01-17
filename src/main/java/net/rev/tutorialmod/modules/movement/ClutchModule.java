package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.ActionResult;

public class ClutchModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private enum ClutchState {
        IDLE,
        STAGE_BLOCK,
        STAGE_WATER,
        DONE
    }

    private ClutchState state = ClutchState.IDLE;
    private int originalSlot = -1;
    private int clutchTicks = 0;
    private int maxTicks = 0;

    // Abort threshold for vertical velocity
    private static final double MAX_VELOCITY = -1.9;

    public void tick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.clutchEnabled) {
            if (state != ClutchState.IDLE) endClutch();
            return;
        }

        var p = mc.player;

        // Abort if landed or falling too fast for placement
        if (p.isOnGround() || p.getVelocity().y < MAX_VELOCITY) {
            if (state != ClutchState.IDLE) endClutch();
            return;
        }

        switch (state) {
            case IDLE -> {
                // Respect configured fall distance and pitch thresholds
                if (p.fallDistance >= TutorialMod.CONFIG.clutchMinFallDistance
                        && p.getPitch() >= TutorialMod.CONFIG.clutchActivationPitch
                        && p.getVelocity().y < -0.15) {

                    BlockHitResult ray = getTargetBlock(TutorialMod.CONFIG.clutchMaxReach);
                    if (ray != null) {
                        originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                        clutchTicks = 0;

                        boolean isWaterloggable = isWaterloggableWhitelist(mc.world.getBlockState(ray.getBlockPos()));
                        if (isWaterloggable) {
                            state = ClutchState.STAGE_BLOCK;
                            maxTicks = 3;
                        } else {
                            state = ClutchState.STAGE_WATER;
                            // Scale hold duration based on fall distance for reliability
                            maxTicks = Math.min(15, 6 + (int)(p.fallDistance / 2));
                        }
                    }
                }
            }

            case STAGE_BLOCK -> {
                int blockSlot = findPlaceableBlock();
                if (blockSlot != -1) {
                    ((PlayerInventoryMixin) p.getInventory()).setSelectedSlot(blockSlot);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();

                    attemptPlacement(p, Direction.UP);
                    clutchTicks++;

                    if (clutchTicks >= maxTicks) {
                        clutchTicks = 0;
                        state = ClutchState.STAGE_WATER;
                        // Set window for the next stage
                        maxTicks = Math.min(15, 6 + (int)(p.fallDistance / 2));
                    }
                } else {
                    // Fallback to water if no block is available
                    clutchTicks = 0;
                    state = ClutchState.STAGE_WATER;
                    maxTicks = Math.min(15, 6 + (int)(p.fallDistance / 2));
                }
            }

            case STAGE_WATER -> {
                int waterSlot = findWaterBucket();
                if (waterSlot != -1) {
                    ((PlayerInventoryMixin) p.getInventory()).setSelectedSlot(waterSlot);
                    ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();

                    attemptPlacement(p, Direction.UP);
                    clutchTicks++;

                    // Crude check for success (bucket emptied). End sequence immediately if placed.
                    if (p.getMainHandStack().isOf(Items.BUCKET) || p.getOffHandStack().isOf(Items.BUCKET)) {
                        state = ClutchState.DONE;
                    } else if (clutchTicks >= maxTicks) {
                        state = ClutchState.DONE;
                    }
                } else {
                    state = ClutchState.DONE;
                }
            }

            case DONE -> {
                endClutch();
            }
        }
    }

    private void attemptPlacement(ClientPlayerEntity p, Direction face) {
        // Use 0.0f for tickDelta to use exact current position during high-speed fall
        HitResult hit = p.raycast(TutorialMod.CONFIG.clutchMaxReach, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult bhr = (BlockHitResult) hit;
        BlockState blockState = mc.world.getBlockState(bhr.getBlockPos());

        // Auto-sneak for interactable blocks
        boolean interactable = isInteractable(blockState);
        if (interactable) {
            mc.options.sneakKey.setPressed(true);
        }

        BlockHitResult placeHit = new BlockHitResult(
            bhr.getPos(),
            face,
            bhr.getBlockPos(),
            false
        );

        ActionResult result = mc.interactionManager.interactBlock(p, Hand.MAIN_HAND, placeHit);

        if (result.isAccepted()) {
            p.swingHand(Hand.MAIN_HAND);
        }
    }

    private void endClutch() {
        if (mc.player != null) {
            if (originalSlot != -1) {
                ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(originalSlot);
                ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
            }
            mc.options.sneakKey.setPressed(isManualSneakPressed());
        }
        originalSlot = -1;
        clutchTicks = 0;
        state = ClutchState.IDLE;
    }

    private BlockHitResult getTargetBlock(double reach) {
        HitResult hit = mc.player.raycast(reach, 0.0f, false);
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
