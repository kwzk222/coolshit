package net.rev.tutorialmod.modules;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.rev.tutorialmod.TutorialMod;

import java.util.ArrayList;
import java.util.List;

public class AutoCobwebFeature {
    private static final double MAX_RANGE = 5.0;

    public static void trigger() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        client.player.sendMessage(Text.literal("AutoCobweb triggered. Scanning for targets..."), false);

        PlayerEntity self = client.player;
        List<PlayerEntity> validTargets = new ArrayList<>();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == self) continue;
            if (TutorialMod.CONFIG.teamManager.isTeammate(player.getName().getString())) continue;
            if (self.distanceTo(player) > MAX_RANGE) continue;

            validTargets.add(player);
        }

        if (validTargets.isEmpty()) {
            client.player.sendMessage(Text.literal("No valid targets found."), false);
            return;
        }

        PlayerEntity bestTarget = null;
        double minAngle = Double.MAX_VALUE;

        Vec3d selfEyePos = self.getEyePos();
        Vec3d lookVec = self.getRotationVector();

        for (PlayerEntity target : validTargets) {
            Vec3d directionToTarget = target.getPos().subtract(selfEyePos).normalize();
            double angle = Math.acos(lookVec.dotProduct(directionToTarget));
            if (angle < minAngle) {
                minAngle = angle;
                bestTarget = target;
            }
        }

        if (bestTarget != null) {
            client.player.sendMessage(Text.literal("Target found: " + bestTarget.getName().getString()), false);
            BlockPos targetBlockPos = bestTarget.getBlockPos().down();

            Vec3d aimTarget = findVisiblePointOnBlock(selfEyePos, targetBlockPos);

            if (aimTarget == null) {
                client.player.sendMessage(Text.literal("Could not find a visible spot on the target block."), false);
                return;
            }

            client.player.sendMessage(Text.literal("Aiming at " + aimTarget), false);

            // Calculate yaw and pitch
            Vec3d d = aimTarget.subtract(selfEyePos).normalize();
            float yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x))) - 90f;
            float pitch = (float) (-Math.toDegrees(Math.asin(d.y)));

            // Set rotation
            self.setYaw(yaw);
            self.setPitch(pitch);

            // Place block
            client.player.sendMessage(Text.literal("Placing cobweb."), false);
            BlockHitResult hitResult = new BlockHitResult(aimTarget, Direction.UP, targetBlockPos, false);
            if (client.interactionManager != null) {
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            }
        } else {
            client.player.sendMessage(Text.literal("No valid targets found."), false);
        }
    }

    private static Vec3d findVisiblePointOnBlock(Vec3d playerEyes, BlockPos block) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;

        // Check center of top face first
        Vec3d centerPoint = block.toCenterPos().add(0, 0.5, 0);
        if (isPointVisible(playerEyes, centerPoint, block)) {
            return centerPoint;
        }

        // Check edges of top face
        for (double i = 0.1; i <= 0.4; i += 0.1) {
            // Check N, S, E, W edges
            Vec3d p1 = new Vec3d(block.getX() + 0.5, block.getY() + 1, block.getZ() + 0.5 - i);
            if (isPointVisible(playerEyes, p1, block)) return p1;
            Vec3d p2 = new Vec3d(block.getX() + 0.5, block.getY() + 1, block.getZ() + 0.5 + i);
            if (isPointVisible(playerEyes, p2, block)) return p2;
            Vec3d p3 = new Vec3d(block.getX() + 0.5 - i, block.getY() + 1, block.getZ() + 0.5);
            if (isPointVisible(playerEyes, p3, block)) return p3;
            Vec3d p4 = new Vec3d(block.getX() + 0.5 + i, block.getY() + 1, block.getZ() + 0.5);
            if (isPointVisible(playerEyes, p4, block)) return p4;
        }

        return null; // No visible point found
    }

    private static boolean isPointVisible(Vec3d from, Vec3d to, BlockPos targetBlock) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        RaycastContext context = new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, client.player);
        BlockHitResult hitResult = client.world.raycast(context);

        return hitResult.getType() == HitResult.Type.BLOCK &&
               hitResult.getBlockPos().equals(targetBlock) &&
               hitResult.getSide() == Direction.UP;
    }
}
