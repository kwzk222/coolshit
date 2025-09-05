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

public class AutoToolSwitch {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private int originalSlot = -1;
    private boolean isMining = false;

    public void onBlockBreak(BlockPos pos) {
        if (!TutorialMod.CONFIG.autoToolSwitchEnabled || mc.player == null || mc.world == null || mc.getNetworkHandler() == null || isMining) {
            return;
        }

        originalSlot = ((PlayerInventoryMixin) mc.player.getInventory()).getSelectedSlot();

        int bestSlot = findBestTool(pos);
        if (bestSlot != -1) {
            if (TutorialMod.CONFIG.toolDurabilitySafetyEnabled) {
                ItemStack bestTool = mc.player.getInventory().getStack(bestSlot);
                if (bestTool.isDamageable() && bestTool.getDamage() >= bestTool.getMaxDamage() - 1) {
                    // Cancel mining
                    mc.getNetworkHandler().sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, pos, Direction.UP));
                    return;
                }
            }
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(bestSlot);
            isMining = true;
        }
    }

    public void onStoppedMining() {
        if (!isMining || mc.player == null) {
            return;
        }

        if (originalSlot != -1) {
            ((PlayerInventoryMixin) mc.player.getInventory()).setSelectedSlot(originalSlot);
            originalSlot = -1;
        }
        isMining = false;
    }

    private int findBestTool(BlockPos pos) {
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
