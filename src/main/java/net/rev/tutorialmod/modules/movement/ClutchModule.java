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
        RECOVERING,
        FINISHING
    }

    private ClutchState state = ClutchState.IDLE;
    private int originalSlot = -1;
    private int tickCounter = 0;

    private static final int RECOVERY_DELAY = 20;
    private static final int MAX_RECOVERY_TIME = 60;
    private static final int POST_GROUND_TICKS = 3;
    private static final int SLOT_RESTORE_DELAY = 5;

    public void tick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.clutchEnabled) {
            if (state != ClutchState.IDLE) reset();
            return;
        }

        var p = mc.player;

        switch (state) {
            case IDLE -> {
                if (!p.isOnGround() && p.fallDistance >= TutorialMod.CONFIG.clutchMinFallDistance && p.getPitch() >= TutorialMod.CONFIG.clutchActivationPitch) {
                    int waterSlot = findWaterBucket();
                    if (waterSlot != -1) {
                        originalSlot = ((PlayerInventoryMixin) p.getInventory()).getSelectedSlot();
                        setSlot(waterSlot);
                        state = ClutchState.PREARMED;
                        tickCounter = 0;
                    }
                }
            }

            case PREARMED -> {
                // Keep engine armed
                mc.options.useKey.setPressed(true);

                if (p.isOnGround()) {
                    tickCounter++;
                    if (waterPlacedBelow(p)) {
                        mc.options.useKey.setPressed(false);
                        tickCounter = 0;
                        state = ClutchState.LANDED;
                    } else if (tickCounter > POST_GROUND_TICKS) {
                        // Failed to clutch after buffer ticks
                        mc.options.useKey.setPressed(false);
                        tickCounter = 0;
                        state = ClutchState.FINISHING;
                    }
                    return;
                }

                // Check for early placement (server ahead of client)
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
                    mc.options.useKey.setPressed(false);
                    tickCounter = 0;
                    state = ClutchState.FINISHING;
                    return;
                }

                int bucketSlot = findEmptyBucket();
                if (bucketSlot != -1) {
                    setSlot(bucketSlot);
                    mc.options.useKey.setPressed(true);

                    // Success when bucket is full again
                    if (p.getInventory().getStack(bucketSlot).isOf(Items.WATER_BUCKET)) {
                        mc.options.useKey.setPressed(false);
                        tickCounter = 0;
                        state = ClutchState.FINISHING;
                    }
                } else {
                    mc.options.useKey.setPressed(false);
                    tickCounter = 0;
                    state = ClutchState.FINISHING;
                }
            }

            case FINISHING -> {
                tickCounter++;
                if (tickCounter >= SLOT_RESTORE_DELAY) {
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
