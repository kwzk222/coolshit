package net.rev.tutorialmod.modules;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
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

            double fallDistance = client.player.fallDistance;
            if (fallDistance < TutorialMod.CONFIG.minFallDistanceClutch || clutchTriggered) return;

            if (!isLandingSoon(client.player)) return;

            if (!(client.crosshairTarget instanceof BlockHitResult hit)) return;

            if (!isLandingUnsafe(client.player)) return;

            // Refined Placement Validation
            BlockPos supportPos = hit.getBlockPos();
            if (!client.player.getWorld().getBlockState(supportPos).isSideSolidFullSquare(client.player.getWorld(), supportPos, hit.getSide())) return;

            BlockPos placePos = supportPos.offset(hit.getSide());
            if (!client.player.getWorld().getBlockState(placePos).isAir()) return;

            int slot = findBestClutchSlot(client.player.getInventory());
            if (slot == -1) return;

            ((net.rev.tutorialmod.mixin.PlayerInventoryMixin) client.player.getInventory()).setSelectedSlot(slot);

            // Simulate a right-click
            client.options.useKey.setPressed(true);
            client.execute(() -> client.options.useKey.setPressed(false));

            clutchTriggered = true;
        });
    }

    private boolean isLandingSoon(ClientPlayerEntity player) {
        Vec3d vel = player.getVelocity();
        if (vel.y >= 0) return false;

        double ticksToImpact = player.getY() / -vel.y;
        return ticksToImpact < 6; // ~0.3s
    }

    private boolean isLandingUnsafe(ClientPlayerEntity player) {
        World world = player.getWorld();
        Vec3d start = player.getPos();
        Vec3d end = start.add(0, -8.0, 0);

        BlockHitResult groundHit = world.raycast(new RaycastContext(
                start,
                end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        BlockPos landingPos = groundHit.getBlockPos();
        BlockState landingState = world.getBlockState(landingPos);

        return landingState.isAir() ||
                landingState.isOf(Blocks.LAVA) ||
                landingState.isOf(Blocks.CACTUS) ||
                landingState.isOf(Blocks.MAGMA_BLOCK);
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
