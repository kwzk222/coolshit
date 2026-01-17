package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;

public class ClutchModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private enum ClutchState {
        IDLE,
        PREARMED,
        LANDED,
        RECOVERING
    }

    private ClutchState state = ClutchState.IDLE;
    private int originalSlot = -1;
    private int tickCounter = 0;

    private static final int RECOVERY_DELAY = 20;
    private static final int MAX_RECOVERY_TIME = 60;

    public void tick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.clutchEnabled) {
            if (state != ClutchState.IDLE) reset();
            return;
        }

        var p = mc.player;

        switch (state) {
            case IDLE -> {
                // Trigger at any height once threshold is met
                if (!p.isOnGround() && p.fallDistance >= TutorialMod.CONFIG.clutchMinFallDistance && p.getPitch() >= TutorialMod.CONFIG.clutchActivationPitch) {
                    int waterSlot = findWaterBucket();
                    if (waterSlot != -1) {
                        originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                        setSlot(waterSlot);
                        state = ClutchState.PREARMED;
                    }
                }
            }

            case PREARMED -> {
                if (p.isOnGround()) {
                    if (waterPlacedBelow(p)) {
                        mc.options.useKey.setPressed(false);
                        tickCounter = 0;
                        state = ClutchState.LANDED;
                    } else {
                        reset();
                    }
                    return;
                }

                // Arm the engine - it will raycast and place when in range (3-4 blocks)
                mc.options.useKey.setPressed(true);

                // If we detect water while still falling (e.g. placed it early)
                if (waterPlacedBelow(p)) {
                    mc.options.useKey.setPressed(false);
                    tickCounter = 0;
                    state = ClutchState.LANDED;
                }
            }

            case LANDED -> {
                tickCounter++;
                if (tickCounter >= RECOVERY_DELAY) {
                    tickCounter = 0;
                    state = ClutchState.RECOVERING;
                }
            }

            case RECOVERING -> {
                tickCounter++;
                if (tickCounter >= MAX_RECOVERY_TIME) {
                    reset();
                    return;
                }

                int bucketSlot = findEmptyBucket();
                if (bucketSlot != -1) {
                    setSlot(bucketSlot);
                    mc.options.useKey.setPressed(true);

                    // Success when bucket is full again
                    if (p.getInventory().getStack(bucketSlot).isOf(Items.WATER_BUCKET)) {
                        reset();
                    }
                } else {
                    reset();
                }
            }
        }
    }

    private void setSlot(int slot) {
        if (mc.player != null && ((PlayerInventoryMixin) mc.player.getInventory()).getSelectedSlot() != slot) {
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(slot);
            ((ClientPlayerInteractionManagerAccessor) mc.interactionManager).invokeSyncSelectedSlot();
        }
    }

    private boolean waterPlacedBelow(net.minecraft.client.network.ClientPlayerEntity p) {
        BlockPos pos = p.getBlockPos();
        // Check current, below, and slightly above for reliability
        return mc.world.getFluidState(pos).isOf(Fluids.WATER) ||
               mc.world.getFluidState(pos.down()).isOf(Fluids.WATER) ||
               mc.world.getFluidState(pos.up()).isOf(Fluids.WATER);
    }

    private void reset() {
        mc.options.useKey.setPressed(isManualUsePressed());
        if (originalSlot != -1 && mc.player != null) {
            setSlot(originalSlot);
            originalSlot = -1;
        }
        state = ClutchState.IDLE;
        tickCounter = 0;
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

    private boolean isManualUsePressed() {
        try {
            return net.minecraft.client.util.InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                    net.minecraft.client.util.InputUtil.fromTranslationKey(mc.options.useKey.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
    }
}
