package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.ModConfig;
import net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor;
import net.rev.tutorialmod.mixin.MinecraftClientAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.fluid.Fluids;
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
        ARMING,
        PLACING_WATER,
        PLACING_WIND_CHARGE,
        LANDED,
        RECOVERING,
        FINISHING
    }

    private ClutchState state = ClutchState.IDLE;
    private boolean isWindClutch = false;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private int spamTickCounter = 0;
    private boolean forcedSneaking = false;

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

        if (state != ClutchState.IDLE) {
            handleInteractableSneak(p);
        }

        switch (state) {
            case IDLE -> {
                // IMPROVED DETECTION: check if falling and close to ground
                if (!p.isOnGround() && p.getVelocity().y < -0.5 && p.getPitch() >= config.clutchActivationPitch) {

                    // Downward raycast to check distance to impact
                    Vec3d start = p.getCameraPosVec(1.0f);
                    Vec3d end = start.add(0, -6.0, 0);
                    BlockHitResult hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
                        start, end,
                        net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                        net.minecraft.world.RaycastContext.FluidHandling.NONE,
                        p
                    ));

                    if (hit.getType() == HitResult.Type.BLOCK) {
                        double dist = start.y - hit.getPos().y;
                        if (dist > 2.0 && dist < 5.5) {
                            int waterSlot = findWaterBucket();
                            int windSlot = findWindCharge();

                            boolean waterAvailable = config.waterClutchEnabled && (config.clutchAutoSwitch ? waterSlot != -1 : isHoldingWater());
                            boolean windAvailable = config.windChargeClutchEnabled && (config.clutchAutoSwitch ? windSlot != -1 : isHoldingWindCharge());

                            int bestSlot = -1;
                            boolean useWater = false;

                            if (config.clutchPriority.equals("Water")) {
                                if (waterAvailable) { useWater = true; bestSlot = waterSlot; }
                                else if (windAvailable) { useWater = false; bestSlot = windSlot; }
                            } else {
                                if (windAvailable) { useWater = false; bestSlot = windSlot; }
                                else if (waterAvailable) { useWater = true; bestSlot = waterSlot; }
                            }

                            if (bestSlot != -1 || (!config.clutchAutoSwitch && (waterAvailable || windAvailable))) {
                                originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                                state = ClutchState.ARMING;
                                isWindClutch = !useWater;
                                if (config.clutchAutoSwitch && bestSlot != -1) setSlot(bestSlot);
                                tickCounter = 0;
                            }
                        }
                    }
                }
            }

            case ARMING -> {
                if (p.isOnGround()) { reset(); return; }
                tickCounter++;
                handleArming(p, config);
            }

            case PLACING_WATER -> {
                if (p.isOnGround()) {
                    state = ClutchState.LANDED;
                    tickCounter = 0;
                    return;
                }
                spamUse();
                spamTickCounter++;
                if (spamTickCounter > MAX_SPAM_TICKS) reset();
            }

            case PLACING_WIND_CHARGE -> {
                if (p.isOnGround()) {
                    state = ClutchState.LANDED;
                    tickCounter = 0;
                    return;
                }
                tickCounter++;
                if (tickCounter > 60) reset();
            }

            case LANDED -> {
                tickCounter++;
                if (tickCounter >= config.clutchRecoveryDelay) {
                    tickCounter = 0;
                    state = isWindClutch ? ClutchState.FINISHING : ClutchState.RECOVERING;
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
                if (tickCounter >= config.clutchRestoreDelay) reset();
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
            mc.options.sneakKey.setPressed(isManualSneakPressed());
            forcedSneaking = false;
        }
    }

    private boolean isInteractable(BlockState state) {
        return state.getBlock() instanceof BlockEntityProvider || state.getBlock() instanceof InventoryProvider;
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

    private void handleArming(net.minecraft.client.network.ClientPlayerEntity p, ModConfig config) {
        double fallVelocity = -p.getVelocity().y;
        if (fallVelocity < 0.1) return;

        Vec3d start = p.getCameraPosVec(1.0f);
        Vec3d end = start.add(0, -6.0, 0);
        BlockHitResult hit = mc.world.raycast(new net.minecraft.world.RaycastContext(
            start, end,
            net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
            net.minecraft.world.RaycastContext.FluidHandling.NONE,
            p
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            double dist = start.y - hit.getPos().y;
            if (isWindClutch) {
                if (dist / fallVelocity <= 2.2) {
                    state = ClutchState.PLACING_WIND_CHARGE;
                    spamUse();
                    tickCounter = 0;
                }
            } else {
                if (dist < 4.5) {
                    state = ClutchState.PLACING_WATER;
                    spamUse();
                    spamTickCounter = 0;
                    tickCounter = 0;
                }
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
        state = ClutchState.IDLE;
        tickCounter = 0;
        spamTickCounter = 0;
        isWindClutch = false;
    }

    private boolean isHoldingWater() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.WATER_BUCKET);
    }

    private boolean isHoldingWindCharge() {
        if (mc.player == null) return false;
        return mc.player.getMainHandStack().isOf(Items.WIND_CHARGE);
    }

    private boolean isManualSneakPressed() {
        try {
            return InputUtil.isKeyPressed(mc.getWindow(),
                InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
    }

    private int findWindCharge() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.WIND_CHARGE)) return i;
        }
        return -1;
    }

    private int findWaterBucket() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.WATER_BUCKET)) return i;
        }
        return -1;
    }

    private int findEmptyBucket() {
        if (mc.player == null) return -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.BUCKET)) return i;
        }
        return -1;
    }
}
