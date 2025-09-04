package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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

            BlockHitResult hitResult = findVisibleHitOnBlock(self, bestTarget);

            if (hitResult == null) {
                client.player.sendMessage(Text.literal("Could not find a visible spot on the target block."), false);
                return;
            }

            Vec3d aimTarget = hitResult.getPos();
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
            if (client.interactionManager != null) {
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            }
        } else {
            client.player.sendMessage(Text.literal("No valid targets found."), false);
        }
    }

    private static BlockHitResult findVisibleHitOnBlock(PlayerEntity self, PlayerEntity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return null;

        Vec3d eyePos = self.getEyePos();
        BlockPos block = target.getBlockPos().down();

        // Offsets to sample around the center of the block's top face
        double[] offsets = {-0.3, 0.0, 0.3};

        for (double dx : offsets) {
            for (double dz : offsets) {
                Vec3d candidate = new Vec3d(
                    block.getX() + 0.5 + dx,
                    block.getY() + 1.0,
                    block.getZ() + 0.5 + dz
                );

                // --- Step 1: Block raycast ---
                RaycastContext rayCtx = new RaycastContext(
                    eyePos, candidate,
                    RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE,
                    self
                );
                BlockHitResult blockHit = client.world.raycast(rayCtx);

                if (blockHit.getType() != HitResult.Type.BLOCK || !blockHit.getBlockPos().equals(block) || blockHit.getSide() != Direction.UP) {
                    // self.sendMessage(Text.literal("Candidate: " + candidate + " -> FAILED (Block Raycast)"), false);
                    continue;
                }

                // --- Step 2: Entity check ---
                if (isLineBlockedByEntities(eyePos, candidate, self, target)) {
                    // self.sendMessage(Text.literal("Candidate: " + candidate + " -> FAILED (Entity Obstruction)"), false);
                    continue; // blocked by some other entity
                }

                self.sendMessage(Text.literal("Candidate: " + candidate + " -> passed"), false);
                // Passed both checks → return adjusted hit
                return new BlockHitResult(
                    candidate, Direction.UP, block, false
                );
            }
        }
        return null;
    }

    private static boolean isLineBlockedByEntities(Vec3d from, Vec3d to, PlayerEntity self, PlayerEntity target) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return false;

        Box searchBox = new Box(from, to).expand(0.1);

        for (Entity e : client.world.getOtherEntities(self, searchBox)) {
            if (e == self || e == target) continue;
            if (e.isSpectator()) continue;

            if (e.getBoundingBox().expand(0.1).raycast(from, to).isPresent()) {
                return true; // blocked
            }
        }
        return false; // clear
    }
}
