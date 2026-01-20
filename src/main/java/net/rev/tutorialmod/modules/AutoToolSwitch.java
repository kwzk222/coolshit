package net.rev.tutorialmod.modules;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;

import java.util.Random;

public class AutoToolSwitch {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();

    // State
    private int originalSlot = -1;
    private boolean isMining = false;
    private int miningDelayTicks = -1;
    private int switchBackDelayTicks = -1;
    private int bestSlotToSwitch = -1;
    private BlockPos currentMiningPos = null;

    public void onTick() {
        if (mc.player == null) {
            reset();
            return;
        }

        if (miningDelayTicks > 0) {
            miningDelayTicks--;
        } else if (miningDelayTicks == 0) {
            performSwitch();
            miningDelayTicks = -1;
        }

        if (switchBackDelayTicks > 0) {
            switchBackDelayTicks--;
        } else if (switchBackDelayTicks == 0) {
            performSwitchBack();
            switchBackDelayTicks = -1;
        }
    }

    public void onBlockBreak(BlockPos pos) {
        if (!TutorialMod.CONFIG.autoToolSwitchEnabled || mc.player == null || mc.world == null || mc.getNetworkHandler() == null || (isMining && pos.equals(currentMiningPos))) {
            return;
        }

        if (!isMining) {
            originalSlot = ((PlayerInventoryMixin) mc.player.getInventory()).getSelectedSlot();
        }

        bestSlotToSwitch = findBestTool(pos);
        currentMiningPos = pos;

        if (bestSlotToSwitch != -1 && bestSlotToSwitch != ((PlayerInventoryMixin) mc.player.getInventory()).getSelectedSlot()) {
            int minDelay = TutorialMod.CONFIG.autoToolSwitchMineMinDelay;
            int maxDelay = TutorialMod.CONFIG.autoToolSwitchMineMaxDelay;
            miningDelayTicks = minDelay + (maxDelay > minDelay ? random.nextInt(maxDelay - minDelay + 1) : 0);
            if (miningDelayTicks == 0) {
                performSwitch();
            }
        }
    }

    public void onStoppedMining() {
        if (!isMining) {
            if (miningDelayTicks != -1) {
                reset();
            }
            return;
        }

        if (TutorialMod.CONFIG.autoToolSwitchBackEnabled) {
            int minDelay = TutorialMod.CONFIG.autoToolSwitchBackMinDelay;
            int maxDelay = TutorialMod.CONFIG.autoToolSwitchBackMaxDelay;
            switchBackDelayTicks = minDelay + (maxDelay > minDelay ? random.nextInt(maxDelay - minDelay + 1) : 0);
            if (switchBackDelayTicks == 0) {
                performSwitchBack();
            }
        } else {
            reset();
        }
    }

    private void performSwitch() {
        if (bestSlotToSwitch != -1 && mc.player != null && mc.getNetworkHandler() != null) {
            if (TutorialMod.CONFIG.toolDurabilitySafetyEnabled) {
                ItemStack bestTool = mc.player.getInventory().getStack(bestSlotToSwitch);
                if (bestTool.isDamageable() && bestTool.getDamage() >= bestTool.getMaxDamage() - 1) {
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, currentMiningPos, Direction.UP));
                    reset();
                    return;
                }
            }
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(bestSlotToSwitch);
            isMining = true;
            miningDelayTicks = -1;
            mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, currentMiningPos, Direction.UP));
        }
    }

    private void performSwitchBack() {
        if (originalSlot != -1 && mc.player != null) {
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(originalSlot);
        }
        reset();
    }

    private void reset() {
        originalSlot = -1;
        isMining = false;
        miningDelayTicks = -1;
        switchBackDelayTicks = -1;
        bestSlotToSwitch = -1;
    }

    private int findBestTool(BlockPos pos) {
        if (mc.player == null || mc.world == null) return -1;
        PlayerInventory inventory = mc.player.getInventory();
        BlockState blockState = mc.world.getBlockState(pos);
        float bestSpeed = 1.0f;
        int bestSlot = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            float speed = getMiningSpeed(stack, blockState);
            if (speed > bestSpeed) {
                bestSpeed = speed;
                bestSlot = i;
            }
        }
        return bestSlot;
    }

    private float getMiningSpeed(ItemStack stack, BlockState state) {
        return stack.getMiningSpeedMultiplier(state);
    }
}
