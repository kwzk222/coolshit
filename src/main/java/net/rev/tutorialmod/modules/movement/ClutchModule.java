package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor;
import net.rev.tutorialmod.mixin.MinecraftClientAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class ClutchModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private enum ClutchState {
        IDLE,
        ARMING,      // Detected fall + pitch, waiting for switch delay
        CLUTCHING,   // Spamming doItemUse
        LANDED,      // Grounded, waiting for recovery delay
        RECOVERING,  // Picking up water
        FINISHING    // Waiting for restore delay
    }

    private ClutchState state = ClutchState.IDLE;
    private int originalSlot = -1;
    private int tickCounter = 0;
    private int spamTickCounter = 0;

    private static final int MAX_SPAM_TICKS = 15;
    private static final int POST_GROUND_TICKS = 3;

    public void tick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.clutchEnabled) {
            if (state != ClutchState.IDLE) reset();
            return;
        }

        var p = mc.player;

        // Disable during flight
        if (p.getAbilities().flying) {
            if (state != ClutchState.IDLE) reset();
            return;
        }

        switch (state) {
            case IDLE -> {
                if (!p.isOnGround() && p.fallDistance >= TutorialMod.CONFIG.clutchMinFallDistance && p.getPitch() >= TutorialMod.CONFIG.clutchActivationPitch) {
                    tickCounter = 0;
                    state = ClutchState.ARMING;
                }
            }

            case ARMING -> {
                if (p.isOnGround()) {
                    reset();
                    return;
                }

                tickCounter++;
                if (tickCounter >= TutorialMod.CONFIG.clutchSwitchDelay) {
                    if (TutorialMod.CONFIG.clutchAutoSwitch) {
                        int waterSlot = findWaterBucket();
                        if (waterSlot != -1) {
                            if (originalSlot == -1) originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                            setSlot(waterSlot);
                        }
                    }

                    tickCounter = 0;
                    spamTickCounter = 0;
                    state = ClutchState.CLUTCHING;
                }
            }

            case CLUTCHING -> {
                if (p.isOnGround()) {
                    tickCounter++;
                    if (waterPlacedBelow(p)) {
                        tickCounter = 0;
                        state = ClutchState.LANDED;
                    } else if (tickCounter > POST_GROUND_TICKS) {
                        tickCounter = 0;
                        state = ClutchState.FINISHING;
                    }
                    return;
                }

                // Interaction logic
                boolean holdingBucket = p.getInventory().getStack(((PlayerInventoryMixin) p.getInventory()).getSelectedSlot()).isOf(Items.WATER_BUCKET);

                if (holdingBucket && !waterPlacedBelow(p)) {
                    spamUse();
                    spamTickCounter++;
                }

                // Fail-safe timeout for mid-air spam
                if (spamTickCounter > MAX_SPAM_TICKS) {
                    tickCounter = 0;
                    state = ClutchState.FINISHING;
                }

                // Early detect success
                if (waterPlacedBelow(p)) {
                    tickCounter = 0;
                    state = ClutchState.LANDED;
                }
            }

            case LANDED -> {
                tickCounter++;
                if (tickCounter >= TutorialMod.CONFIG.clutchRecoveryDelay) {
                    tickCounter = 0;
                    state = ClutchState.RECOVERING;
                }
            }

            case RECOVERING -> {
                tickCounter++;
                if (tickCounter >= 60) { // Max recovery time fail-safe
                    tickCounter = 0;
                    state = ClutchState.FINISHING;
                    return;
                }

                int bucketSlot = findEmptyBucket();
                if (bucketSlot != -1) {
                    setSlot(bucketSlot);
                    spamUse();

                    if (p.getInventory().getStack(bucketSlot).isOf(Items.WATER_BUCKET)) {
                        tickCounter = 0;
                        state = ClutchState.FINISHING;
                    }
                } else {
                    // It might be that the bucket is already full or we don't have one
                    tickCounter = 0;
                    state = ClutchState.FINISHING;
                }
            }

            case FINISHING -> {
                tickCounter++;
                if (tickCounter >= TutorialMod.CONFIG.clutchRestoreDelay) {
                    if (TutorialMod.CONFIG.clutchRestoreOriginalSlot && originalSlot != -1) {
                        setSlot(originalSlot);
                    }
                    originalSlot = -1;
                    state = ClutchState.IDLE;
                    tickCounter = 0;
                }
            }
        }
    }

    private void spamUse() {
        // reset cooldown so every tick is valid
        ((MinecraftClientAccessor) mc).setItemUseCooldown(0);

        // true RMB press (full pipeline)
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
        if (originalSlot != -1 && mc.player != null) {
            // Only switch back if configured, but always clear the variable
            if (TutorialMod.CONFIG.clutchRestoreOriginalSlot) {
                setSlot(originalSlot);
            }
            originalSlot = -1;
        }
        state = ClutchState.IDLE;
        tickCounter = 0;
        spamTickCounter = 0;
    }

    private int findWaterBucket() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.WATER_BUCKET))
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
