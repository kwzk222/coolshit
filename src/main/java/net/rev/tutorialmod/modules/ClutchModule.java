package net.rev.tutorialmod.modules;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.rev.tutorialmod.TutorialMod;

import java.util.List;

public class ClutchModule {
    private static final List<Block> CLUTCH_PRIORITY = List.of(
            Blocks.SLIME_BLOCK,
            Blocks.COBWEB,
            Blocks.HAY_BLOCK
    );

    private boolean clutchTriggered;

    public void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || !TutorialMod.CONFIG.clutchModuleEnabled) return;

            if (client.player.isOnGround() || client.player.getAbilities().flying) {
                clutchTriggered = false;
                return;
            }

            if (client.player.fallDistance < TutorialMod.CONFIG.minFallDistanceClutch || clutchTriggered) return;

            if (!(client.crosshairTarget instanceof BlockHitResult hit)) return;

            BlockPos supportPos = hit.getBlockPos();
            if (!client.player.getWorld().getBlockState(supportPos).isSideSolidFullSquare(client.player.getWorld(), supportPos, hit.getSide())) return;

            BlockPos placePos = supportPos.offset(hit.getSide());
            if (!client.player.getWorld().getBlockState(placePos).isAir()) return;

            int slot = findBestClutchSlot(client.player.getInventory());
            if (slot == -1) return;

            clutchTriggered = true;
            ((net.rev.tutorialmod.mixin.PlayerInventoryMixin) client.player.getInventory()).setSelectedSlot(slot);

            client.options.useKey.setPressed(true);
            client.execute(() -> {
                client.options.useKey.setPressed(true);
                client.execute(() -> client.options.useKey.setPressed(false));
            });
        });
    }

    private int findBestClutchSlot(PlayerInventory inv) {
        for (Block block : CLUTCH_PRIORITY) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = inv.getStack(i);
                if (stack.getItem() instanceof BlockItem bi &&
                        bi.getBlock() == block) {
                    return i;
                }
            }
        }
        return -1;
    }
}
