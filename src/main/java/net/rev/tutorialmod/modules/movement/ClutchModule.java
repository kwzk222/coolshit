package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.ModConfig;
import net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor;
import net.rev.tutorialmod.mixin.MinecraftClientAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.minecraft.block.*;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public class ClutchModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private enum ClutchState {
        IDLE,
        ARMING,          // Detected fall, selecting first item
        PLACING_BLOCK,   // Spamming block placement
        PLACING_WATER,   // Spamming water placement
        LANDED,          // On ground, waiting to recover
        RECOVERING,      // Picking up water
        FINISHING        // Restoring slot
    }

    private ClutchState state = ClutchState.IDLE;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private int spamTickCounter = 0;
    private boolean forcedSneaking = false;

    private static final int MAX_SPAM_TICKS = 20;

    public void tick() {
        if (mc.player == null || mc.world == null) {
            if (state != ClutchState.IDLE) reset();
            return;
        }

        ModConfig config = TutorialMod.CONFIG;
        var p = mc.player;

        if (!config.masterEnabled || !config.clutchEnabled || p.getAbilities().flying) {
            if (state != ClutchState.IDLE) reset();
            return;
        }

        // Always check for interactable under us to sneak
        handleInteractableSneak(p);

        switch (state) {
            case IDLE -> {
                if (!p.isOnGround() && p.fallDistance >= config.clutchMinFallDistance && p.getPitch() >= config.clutchActivationPitch) {
                    tickCounter = 0;
                    state = ClutchState.ARMING;
                }
            }

            case ARMING -> {
                if (p.isOnGround()) { reset(); return; }

                tickCounter++;
                if (tickCounter >= config.clutchSwitchDelay) {
                    if (originalSlot == -1) originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();

                    HitResult hit = p.raycast(3.2, 1.0f, false);
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        BlockPos pos = ((BlockHitResult) hit).getBlockPos();
                        BlockState blockState = mc.world.getBlockState(pos);

                        if (isWaterloggable(blockState)) {
                            if (config.clutchAutoSwitch) {
                                int waterSlot = findWaterBucket();
                                if (waterSlot != -1) setSlot(waterSlot);
                            }
                            state = ClutchState.PLACING_WATER;
                        } else {
                            if (config.clutchAutoSwitch) {
                                int blockSlot = findBlock();
                                if (blockSlot != -1) {
                                    setSlot(blockSlot);
                                    state = ClutchState.PLACING_BLOCK;
                                } else {
                                    // No block found, try water anyway as a last resort
                                    int waterSlot = findWaterBucket();
                                    if (waterSlot != -1) {
                                        setSlot(waterSlot);
                                        state = ClutchState.PLACING_WATER;
                                    } else {
                                        reset();
                                    }
                                }
                            } else {
                                // Manual switch mode, just proceed to whatever we are holding
                                state = ClutchState.PLACING_BLOCK;
                            }
                        }
                        spamTickCounter = 0;
                        tickCounter = 0;
                    }
                }
            }

            case PLACING_BLOCK -> {
                if (p.isOnGround()) {
                    state = ClutchState.LANDED;
                    tickCounter = 0;
                    return;
                }

                spamUse();
                spamTickCounter++;

                // Check if we successfully placed a block
                HitResult hit = p.raycast(3.2, 1.0f, false);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockPos pos = ((BlockHitResult) hit).getBlockPos();
                    if (mc.world.getBlockState(pos).getBlock() != Blocks.AIR) {
                        // Success placing block, now move to water
                        int waterSlot = findWaterBucket();
                        if (waterSlot != -1) {
                            setSlot(waterSlot);
                            state = ClutchState.PLACING_WATER;
                            spamTickCounter = 0;
                        }
                    }
                }

                if (spamTickCounter > MAX_SPAM_TICKS) { reset(); }
            }

            case PLACING_WATER -> {
                if (p.isOnGround()) {
                    if (waterPlacedBelow(p)) {
                        state = ClutchState.LANDED;
                        tickCounter = 0;
                    } else {
                        // Small buffer for lag
                        tickCounter++;
                        if (tickCounter > 3) reset();
                    }
                    return;
                }

                spamUse();
                spamTickCounter++;

                if (waterPlacedBelow(p)) {
                    state = ClutchState.LANDED;
                    tickCounter = 0;
                }

                if (spamTickCounter > MAX_SPAM_TICKS) { reset(); }
            }

            case LANDED -> {
                tickCounter++;
                if (tickCounter >= config.clutchRecoveryDelay) {
                    tickCounter = 0;
                    state = ClutchState.RECOVERING;
                }
            }

            case RECOVERING -> {
                tickCounter++;
                int bucketSlot = findEmptyBucket();
                if (bucketSlot != -1) {
                    setSlot(bucketSlot);
                    spamUse();

                    if (p.getInventory().getStack(bucketSlot).isOf(Items.WATER_BUCKET)) {
                        state = ClutchState.FINISHING;
                        tickCounter = 0;
                    }
                } else {
                    state = ClutchState.FINISHING;
                    tickCounter = 0;
                }

                if (tickCounter > 40) state = ClutchState.FINISHING;
            }

            case FINISHING -> {
                tickCounter++;
                if (tickCounter >= config.clutchRestoreDelay) {
                    reset();
                }
            }
        }
    }

    private void handleInteractableSneak(net.minecraft.client.network.ClientPlayerEntity p) {
        HitResult hit = p.raycast(3.2, 1.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            BlockState state = mc.world.getBlockState(pos);
            if (isInteractable(state)) {
                mc.options.sneakKey.setPressed(true);
                forcedSneaking = true;
            } else if (forcedSneaking && state.isAir()) { // Only release if we move to air or non-interactable
                // Actually keep sneaking if we are in the middle of a clutch
                if (this.state == ClutchState.IDLE) {
                   mc.options.sneakKey.setPressed(isManualSneakPressed());
                   forcedSneaking = false;
                }
            }
        } else if (forcedSneaking && this.state == ClutchState.IDLE) {
            mc.options.sneakKey.setPressed(isManualSneakPressed());
            forcedSneaking = false;
        }
    }

    private boolean isInteractable(BlockState state) {
        return state.getBlock() instanceof BlockEntityProvider || state.getBlock() instanceof InventoryProvider;
    }

    private boolean isWaterloggable(BlockState state) {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock) return true;
        if (block instanceof StairsBlock) return true;
        if (block instanceof SlabBlock) {
            return state.get(SlabBlock.TYPE) != SlabType.BOTTOM;
        }
        if (block instanceof TrapdoorBlock) {
            return state.get(TrapdoorBlock.HALF) == BlockHalf.TOP && !state.get(TrapdoorBlock.OPEN);
        }
        return block == Blocks.CAULDRON ||
               block == Blocks.BIG_DRIPLEAF ||
               block == Blocks.COPPER_GRATE ||
               block == Blocks.DECORATED_POT ||
               block == Blocks.MANGROVE_ROOTS;
    }

    private void spamUse() {
        ((MinecraftClientAccessor) mc).setItemUseCooldown(0);
        ((MinecraftClientAccessor) mc).invokeDoItemUse();
    }

    private void setSlot(int slot) {
        if (mc.player != null && ((PlayerInventoryMixin) mc.player.getInventory()).getSelectedSlot() != slot) {
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(slot);
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
        }
    }

    private boolean waterPlacedBelow(net.minecraft.client.network.ClientPlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        return mc.world.getFluidState(pos).isOf(Fluids.WATER) ||
               mc.world.getFluidState(pos.down()).isOf(Fluids.WATER) ||
               mc.world.getFluidState(pos.up()).isOf(Fluids.WATER);
    }

    private void reset() {
        if (originalSlot != -1 && mc.player != null && TutorialMod.CONFIG.clutchRestoreOriginalSlot) {
            setSlot(originalSlot);
        }
        if (forcedSneaking) {
            mc.options.sneakKey.setPressed(isManualSneakPressed());
            forcedSneaking = false;
        }
        originalSlot = -1;
        state = ClutchState.IDLE;
        tickCounter = 0;
        spamTickCounter = 0;
    }

    private boolean isManualSneakPressed() {
        try {
            return net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                net.minecraft.client.util.InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
    }

    private int findWaterBucket() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.WATER_BUCKET))
                return i;
        }
        return -1;
    }

    private int findBlock() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem)
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
}
