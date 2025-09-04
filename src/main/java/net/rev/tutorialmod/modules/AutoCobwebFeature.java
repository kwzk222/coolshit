package net.rev.tutorialmod.modules;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Environment(EnvType.CLIENT)
public class AutoCobwebFeature {

    public static void trigger() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        client.execute(() -> {
            if (client.player == null || client.world == null || !TutorialMod.CONFIG.autoCobwebEnabled) return;
            ClientPlayerEntity self = client.player;

            self.sendMessage(Text.literal("[AutoCobweb] Triggered"), false);

            if (self.getMainHandStack().isEmpty() || self.getMainHandStack().getItem() != Items.COBWEB) {
                self.sendMessage(Text.literal("[AutoCobweb] You must hold a cobweb in main hand."), false);
                return;
            }

            List<PlayerEntity> candidates = new ArrayList<>();
            for (PlayerEntity p : client.world.getPlayers()) {
                if (p == self || p.isSpectator() || self.distanceTo(p) > TutorialMod.CONFIG.autoCobwebMaxRange || TutorialMod.CONFIG.teamManager.isTeammate(p.getName().getString())) {
                    continue;
                }
                candidates.add(p);
            }

            if (candidates.isEmpty()) {
                self.sendMessage(Text.literal("[AutoCobweb] No valid targets in range."), false);
                return;
            }

            Vec3d eye = self.getEyePos();
            Vec3d look = self.getRotationVector();
            PlayerEntity bestTarget = candidates.stream()
                    .max(Comparator.comparingDouble(p -> p.getPos().subtract(eye).normalize().dotProduct(look)))
                    .orElse(null);

            if (bestTarget == null) {
                self.sendMessage(Text.literal("[AutoCobweb] Could not select a best target."), false);
                return;
            }

            self.sendMessage(Text.literal("[AutoCobweb] Target selected: " + bestTarget.getName().getString()), false);

            BlockPos targetBlock;
            if (!bestTarget.isOnGround()) {
                targetBlock = predictLandingPos(bestTarget, client);
                self.sendMessage(Text.literal("[AutoCobweb] Target is airborne, predicting landing position."), false);
            } else {
                targetBlock = bestTarget.getBlockPos().down();
            }
            self.sendMessage(Text.literal("[AutoCobweb] Target block: " + targetBlock.toShortString()), false);

            Optional<BlockHitResult> hitResultOpt = findVisibleHitOnBlock(client, self, bestTarget, targetBlock);

            if (hitResultOpt.isEmpty()) {
                self.sendMessage(Text.literal("[AutoCobweb] No visible & unobstructed aim point found."), false);
                return;
            }

            BlockHitResult hitResult = hitResultOpt.get();
            Vec3d aim = hitResult.getPos();
            self.sendMessage(Text.literal("[AutoCobweb] Aiming at " + String.format("%.2f, %.2f, %.2f", aim.x, aim.y, aim.z)), false);

            Vec3d d = aim.subtract(eye).normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f;
            float pitch = (float) -Math.toDegrees(Math.asin(d.y));
            TutorialModClient.originalYaw = self.getYaw();
            TutorialModClient.originalPitch = self.getPitch();

            self.setYaw(yaw);
            self.setPitch(pitch);

            TutorialModClient.autoCobwebPlacementDelay = 1;
        });
    }

    private static Optional<BlockHitResult> findVisibleHitOnBlock(MinecraftClient client, ClientPlayerEntity self, PlayerEntity target, BlockPos block) {
        Vec3d eye = self.getEyePos();
        Vec3d look = self.getRotationVector();

        List<Vec3d> candidatePoints = new ArrayList<>();
        double[] offsets = {-0.35, 0.0, 0.35};
        for (double dx : offsets) {
            for (double dz : offsets) {
                candidatePoints.add(new Vec3d(block.getX() + 0.5 + dx, block.getY() + 0.999, block.getZ() + 0.5 + dz));
            }
        }

        // Find all visible points first
        List<BlockHitResult> visibleHits = new ArrayList<>();
        for (Vec3d candidate : candidatePoints) {
            if (isBlockedByEntity(client, eye, candidate, self, target, eye.squaredDistanceTo(candidate))) {
                continue;
            }

            RaycastContext ctx = new RaycastContext(eye, candidate, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, self);
            BlockHitResult blockHit = client.world.raycast(ctx);

            if (blockHit.getType() == HitResult.Type.BLOCK && blockHit.getBlockPos().equals(block) && blockHit.getSide() == Direction.UP) {
                visibleHits.add(blockHit);
            }
        }

        if (visibleHits.isEmpty()) {
            return Optional.empty();
        }

        // Pick one of the visible points with weighting
        return Optional.of(pickWeightedRandomPoint(eye, look, visibleHits));
    }

    private static boolean isBlockedByEntity(MinecraftClient client, Vec3d from, Vec3d to, PlayerEntity self, PlayerEntity target, double candidateDistSq) {
        if (client.world == null) return false;

        Box searchBox = new Box(from, to).expand(0.1);

        for (Entity e : client.world.getOtherEntities(self, searchBox)) {
            if (e.isSpectator()) continue;

            Optional<Vec3d> entHit = e.getBoundingBox().expand(0.1).raycast(from, to);
            if (entHit.isPresent()) {
                // If we hit our target, it's only a blocker if the hit is closer than the block candidate
                if (e == target) {
                    if (from.squaredDistanceTo(entHit.get()) < candidateDistSq) {
                        return true;
                    }
                } else {
                    // Any other entity is a blocker
                    return true;
                }
            }
        }
        return false;
    }

    private static BlockHitResult pickWeightedRandomPoint(Vec3d playerEyes, Vec3d lookVec, List<BlockHitResult> points) {
        Random rand = new Random();
        double[] weights = new double[points.size()];
        double totalWeight = 0.0;

        for (int i = 0; i < points.size(); i++) {
            Vec3d point = points.get(i).getPos();

            double dist = playerEyes.distanceTo(point);
            double distWeight = 1.0 / (dist + 0.001);

            Vec3d dirToPoint = point.subtract(playerEyes).normalize();
            double align = lookVec.dotProduct(dirToPoint);
            double alignWeight = Math.max(align, 0);

            double weight = distWeight * 0.5 + alignWeight * 0.5;
            weights[i] = weight;
            totalWeight += weight;
        }

        double r = rand.nextDouble() * totalWeight;
        for (int i = 0; i < points.size(); i++) {
            r -= weights[i];
            if (r <= 0) {
                return points.get(i);
            }
        }
        return points.get(points.size() - 1);
    }

    private static BlockPos predictLandingPos(PlayerEntity player, MinecraftClient client) {
        if (client.world == null) {
            return player.getBlockPos().down();
        }

        Vec3d simPos = player.getPos();
        Vec3d simVel = player.getVelocity();

        // Simulate tick-by-tick
        for (int i = 0; i < 200; i++) { // Max 10 seconds prediction
            Vec3d nextPos = simPos.add(simVel);

            RaycastContext raycastContext = new RaycastContext(
                    simPos,
                    nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    player
            );

            BlockHitResult hit = client.world.raycast(raycastContext);

            if (hit.getType() == HitResult.Type.BLOCK) {
                return hit.getBlockPos();
            }

            simPos = nextPos;

            // Apply physics for next tick
            simVel = simVel.multiply(0.98, 0.98, 0.98);
            simVel = simVel.add(0, -0.08, 0);
        }

        // Fallback if no landing spot found (e.g. over void)
        BlockHitResult hit = client.world.raycast(new RaycastContext(
                simPos,
                new Vec3d(simPos.x, client.world.getBottomY(), simPos.z),
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
        ));

        if (hit.getType() == HitResult.Type.BLOCK) {
            return hit.getBlockPos();
        }

        // Absolute fallback
        return player.getBlockPos().down();
    }
}
