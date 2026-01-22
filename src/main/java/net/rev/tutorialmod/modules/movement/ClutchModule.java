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
import net.minecraft.client.util.InputUtil;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class ClutchModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private enum ClutchState {
        IDLE,
        ARMING,          // Detected fall, pre-selecting bucket
        PLACING_BLOCK,   // Falling on waterloggable, placing block first
        PLACING_WATER,   // Placing water (direct or after block)
        LANDED,          // On ground, buffer for lag
        RECOVERING,      // Picking up water
        FINISHING,       // Restoring slot

        // Wind Charge States
        WIND_PREARMED,
        WIND_READY_TO_FIRE,
        WIND_FIRED
    }

    private ClutchState state = ClutchState.IDLE;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private int spamTickCounter = 0;
    private boolean forcedSneaking = false;

    // Wind charge runtime fields
    private int fireAttempts = 0;
    private double vyBeforeFire = 0.0;
    private int successTickCounter = 0;
    private int armedSlot = -1;

    private static final int MAX_SPAM_TICKS = 30;
    private static final double CLUTCH_REACH = 4.0;

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

        // Fix random sneaking: handle interactables during water clutch sequence and recovery
        if (state == ClutchState.ARMING || state == ClutchState.PLACING_BLOCK || state == ClutchState.PLACING_WATER ||
            state == ClutchState.LANDED || state == ClutchState.RECOVERING || state == ClutchState.FINISHING) {
            handleInteractableSneak(p);
        }

        switch (state) {
            case IDLE -> {
                if (!p.isOnGround() && p.getPitch() >= config.clutchActivationPitch) {
                    // Prioritize Water Clutch (or Block Clutch fallback)
                    boolean hasBucket = config.clutchAutoSwitch ? (findWaterBucket() != -1) : isHoldingWater();
                    boolean hasBlock = findBlock() != -1;

                    boolean canWaterClutch = config.waterClutchEnabled && p.fallDistance >= config.clutchMinFallDistance && (hasBucket || hasBlock);

                    if (canWaterClutch) {
                        originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                        tickCounter = 0;
                        state = ClutchState.ARMING;

                        // Immediate first tick of arming/switching
                        if (config.clutchAutoSwitch) {
                            int waterSlot = findWaterBucket();
                            if (waterSlot != -1) setSlot(waterSlot);
                        }
                        tickCounter++;
                        handleArming(p, config);
                    }
                    // Fallback to Wind Charge Clutch
                    else if (config.windClutchEnabled && p.fallDistance >= config.windClutchMinFallDistance) {
                        int slot = findWindChargeSlot();
                        if (slot != -1) {
                            originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                            armedSlot = slot;
                            setSlot(slot);
                            fireAttempts = 0;
                            vyBeforeFire = 0.0;
                            successTickCounter = 0;
                            state = ClutchState.WIND_PREARMED;
                        }
                    }
                }
            }

            case ARMING -> {
                if (p.isOnGround()) { reset(); return; }

                // Continuous pre-arm switch in case of inventory changes
                if (config.clutchAutoSwitch) {
                    int waterSlot = findWaterBucket();
                    if (waterSlot != -1) setSlot(waterSlot);
                }

                tickCounter++;
                handleArming(p, config);
            }

            case PLACING_BLOCK -> {
                if (p.isOnGround()) {
                    state = ClutchState.LANDED;
                    tickCounter = 0;
                    return;
                }

                spamUse();
                spamTickCounter++;

                // Success check: block placed at offset position
                HitResult hit = p.raycast(CLUTCH_REACH, 1.0f, false);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    BlockHitResult bhr = (BlockHitResult) hit;
                    BlockPos placedPos = bhr.getBlockPos().offset(bhr.getSide());
                    if (!mc.world.getBlockState(placedPos).isAir()) {
                        // Success! Now switch to water
                        int waterSlot = findWaterBucket();
                        if (waterSlot != -1) {
                            if (config.clutchAutoSwitch) setSlot(waterSlot);
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
                        // Success buffer
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

            case WIND_PREARMED -> {
                if (p.isOnGround()) { reset(); return; }

                Vec3d vel = p.getVelocity();
                if (vel.y >= -0.08) return; // not falling meaningfully

                Box box = p.getBoundingBox();
                Box nextBox = box.offset(vel.x, vel.y, vel.z);

                // Predict next tick collision to preempt damage resolution
                boolean willCollide = mc.world.getBlockCollisions(p, nextBox).iterator().hasNext();

                if (willCollide) {
                    state = ClutchState.WIND_READY_TO_FIRE;
                }
            }

            case WIND_READY_TO_FIRE -> {
                if (p.isOnGround()) { reset(); return; }
                doWindFireAttempt();
                state = ClutchState.WIND_FIRED;
                vyBeforeFire = p.getVelocity().y;
                successTickCounter = 0;
            }

            case WIND_FIRED -> {
                successTickCounter++;
                double vyNow = p.getVelocity().y;
                boolean velocitySuccess = (vyNow - vyBeforeFire) >= config.windClutchSuccessVyDelta;
                boolean fallDistanceReset = p.fallDistance < Math.max(1.0, config.windClutchMinFallDistance / 2.0);

                if (velocitySuccess || fallDistanceReset) {
                    state = ClutchState.FINISHING;
                    tickCounter = 0;
                    return;
                }

                if (successTickCounter > 8) { reset(); return; }
            }
        }
    }

    private void handleInteractableSneak(net.minecraft.client.network.ClientPlayerEntity p) {
        HitResult hit = p.raycast(CLUTCH_REACH, 1.0f, false);
        if (hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = ((BlockHitResult) hit).getBlockPos();
            BlockState state = mc.world.getBlockState(pos);
            if (isInteractable(state)) {
                mc.options.sneakKey.setPressed(true);
                forcedSneaking = true;
            }
        } else if (forcedSneaking) {
            // Only release if we moved off the interactable
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

    private void handleArming(net.minecraft.client.network.ClientPlayerEntity p, ModConfig config) {
        if (tickCounter >= config.clutchSwitchDelay) {
            // Check target with increased reach (10.0) to transition early
            HitResult hit = p.raycast(10.0, 1.0f, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockState targetState = mc.world.getBlockState(((BlockHitResult) hit).getBlockPos());

                // IF target is waterloggable -> place block FIRST (to avoid waterlogging)
                if (isWaterloggable(targetState)) {
                    int blockSlot = findBlock();
                    if (blockSlot != -1) {
                        if (config.clutchAutoSwitch) setSlot(blockSlot);
                        state = ClutchState.PLACING_BLOCK;
                        spamUse(); // Save a tick
                    } else {
                        state = ClutchState.PLACING_WATER;
                        spamUse(); // Save a tick
                    }
                } else {
                    state = ClutchState.PLACING_WATER;
                    spamUse(); // Save a tick
                }
                spamTickCounter = 0;
                tickCounter = 0;
            }
        }
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
        armedSlot = -1;
        state = ClutchState.IDLE;
        tickCounter = 0;
        spamTickCounter = 0;
        fireAttempts = 0;
        vyBeforeFire = 0.0;
        successTickCounter = 0;
    }

    private void doWindFireAttempt() {
        if (mc.player == null) return;
        if (((PlayerInventoryMixin) mc.player.getInventory()).getSelectedSlot() != armedSlot) {
            setSlot(armedSlot);
        }
        ((MinecraftClientAccessor) mc).setItemUseCooldown(0);
        ((MinecraftClientAccessor) mc).invokeDoItemUse();
        fireAttempts++;
    }


    private int findWindChargeSlot() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) return i;
        }
        return -1;
    }

    private boolean isHoldingWater() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.WATER_BUCKET);
    }

    private boolean isManualSneakPressed() {
        try {
            return InputUtil.isKeyPressed(mc.getWindow(),
                InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode());
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
