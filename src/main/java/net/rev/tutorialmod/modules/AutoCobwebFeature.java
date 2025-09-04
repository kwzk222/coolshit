package net.rev.tutorialmod.modules;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Environment(EnvType.CLIENT)
public class AutoCobwebFeature {
    private static final double MAX_RANGE = 5.0; // placement reach

    public static void trigger() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;

        // Make sure we run on the client thread (safe)
        client.execute(() -> {
            if (client.player == null || client.world == null) return;
            PlayerEntity self = client.player;

            self.sendMessage(Text.literal("[AutoCobweb] Triggered"), false);

            // 1) Build potential target list (debug each)
            List<PlayerEntity> candidates = new ArrayList<>();
            for (PlayerEntity p : client.world.getPlayers()) {
                if (p == self) continue;
                if (p.isSpectator()) continue;
                if (TutorialMod.CONFIG.teamManager.isTeammate(p.getName().getString())) continue;
                double dist = self.distanceTo(p);
                if (dist > MAX_RANGE) {
                    // debug: out of range
                    continue;
                }
                candidates.add(p);
            }

            if (candidates.isEmpty()) {
                self.sendMessage(Text.literal("[AutoCobweb] No players in range/not teammates."), false);
                return;
            }

            // 2) Prefer target that is roughly in your view cone; fallback to nearest
            Vec3d eye = self.getEyePos();
            Vec3d look = self.getRotationVector();
            PlayerEntity best = candidates.stream()
                    .max(Comparator.comparingDouble(p -> {
                        Vec3d to = p.getPos().subtract(eye).normalize();
                        return look.dotProduct(to); // prefer larger dot (more centered)
                    }))
                    .orElse(null);

            if (best == null) {
                self.sendMessage(Text.literal("[AutoCobweb] No target after selection."), false);
                return;
            }

            // Debug: show chosen candidate and angle/distance
            Vec3d toBest = best.getPos().subtract(eye);
            double distBest = toBest.length();
            double angleDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, look.dotProduct(toBest.normalize())))));
            self.sendMessage(Text.literal(String.format("[AutoCobweb] Candidate: %s dist=%.2f angle=%.1f°",
                    best.getName().getString(), distBest, angleDeg)), false);

            // 3) Find a visible point on top-of-block under the target
            BlockPos under = best.getBlockPos().down();
            Optional<Vec3d> visible = findVisiblePointOnTopFace(client, self, best, under);

            if (visible.isEmpty()) {
                self.sendMessage(Text.literal("[AutoCobweb] No unobstructed top-face point found."), false);
                return;
            }

            Vec3d aim = visible.get();
            self.sendMessage(Text.literal("[AutoCobweb] Aiming at: " + aim.toString()), false);

            // 4) Check player is holding cobweb in mainhand (simple and reliable)
            if (self.getMainHandStack().isEmpty() || self.getMainHandStack().getItem() != Items.COBWEB) {
                self.sendMessage(Text.literal("[AutoCobweb] You must hold a cobweb in main hand for automatic placement."), false);
                return;
            }

            // 5) Face the point (client-side rotate)
            Vec3d d = aim.subtract(eye).normalize();
            float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f;
            float pitch = (float) (-Math.toDegrees(Math.asin(d.y)));
            self.setYaw(yaw);
            self.setPitch(pitch);

            self.sendMessage(Text.literal("[AutoCobweb] Attempting placement..."), false);

            // 6) Build BlockHitResult for the top-face (server expects face and pos)
            BlockHitResult bhr = new BlockHitResult(
                    aim,
                    net.minecraft.util.math.Direction.UP,
                    under,
                    false
            );

            // 7) Interact (client-side will send packet to server)
            if (client.interactionManager != null) {
                client.interactionManager.interactBlock(self, Hand.MAIN_HAND, bhr);
                self.sendMessage(Text.literal("[AutoCobweb] interactBlock called."), false);
            } else {
                self.sendMessage(Text.literal("[AutoCobweb] No interaction manager available."), false);
            }
        }); // end client.execute
    }

    /**
     * Samples a 3x3 grid on the top face of `block` and returns the first point that:
     *  - is raycast-hit to the target block top
     *  - is NOT occluded by an entity whose bounding-box hitpoint is closer than the candidate
     */
    private static Optional<Vec3d> findVisiblePointOnTopFace(MinecraftClient client, PlayerEntity self, PlayerEntity target, BlockPos block) {
        if (client.world == null) return Optional.empty();

        Vec3d eye = self.getEyePos();
        double[] offs = {-0.35, 0.0, 0.35}; // change if you want tighter/wider search

        for (double dx : offs) {
            for (double dz : offs) {
                Vec3d candidate = new Vec3d(block.getX() + 0.5 + dx, block.getY() + 1.0, block.getZ() + 0.5 + dz);
                double candDist2 = eye.squaredDistanceTo(candidate);

                // 1) Check entity occlusion: any entity whose bbox intersects the segment sooner than the block?
                if (isBlockedByEntity(client, eye, candidate, self, target, candDist2)) {
                    // debug: blocked by entity
                    self.sendMessage(Text.literal("[AutoCobweb] Candidate " + candidate + " blocked by entity."), false);
                    continue;
                }

                // 2) Raycast to block (block-only) to ensure we hit the target block top
                RaycastContext ctx = new RaycastContext(eye, candidate, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, self);
                HitResult hit = client.world.raycast(ctx);
                if (hit.getType() != HitResult.Type.BLOCK) {
                    self.sendMessage(Text.literal("[AutoCobweb] Candidate " + candidate + " raycast did not hit a block."), false);
                    continue;
                }
                BlockHitResult bhr = (BlockHitResult) hit;
                if (!bhr.getBlockPos().equals(block)) {
                    self.sendMessage(Text.literal("[AutoCobweb] Candidate " + candidate + " hit wrong block: " + bhr.getBlockPos()), false);
                    continue;
                }
                if (bhr.getSide() != Direction.UP) {
                    self.sendMessage(Text.literal("[AutoCobweb] Candidate " + candidate + " hit side " + bhr.getSide()), false);
                    continue;
                }

                // PASS
                self.sendMessage(Text.literal("[AutoCobweb] Candidate OK: " + candidate), false);
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    /**
     * Returns true if any entity (other than self/target) intersects the ray segment
     * earlier than the candidate block hit. Also treats the target's bounding box as blocking
     * (we want a point not occluded by the target).
     */
    private static boolean isBlockedByEntity(MinecraftClient client, Vec3d from, Vec3d to, PlayerEntity self, PlayerEntity target, double candidateDistSq) {
        if (client.world == null) return false;

        // AABB that covers the segment (with a tiny padding)
        Box search = new Box(from, to).expand(0.1);

        for (Entity e : client.world.getOtherEntities(self, search)) {
            if (e == self) continue;
            // we treat the target as a blocker as well (we want a point not blocked by the target)
            if (e == target || e.isSpectator()) {
                Optional<Vec3d> entHit = e.getBoundingBox().raycast(from, to);
                if (entHit.isPresent()) {
                    double d2 = from.squaredDistanceTo(entHit.get());
                    if (d2 < candidateDistSq - 1e-6) return true;
                }
                continue;
            }

            // other entities: check if they intersect the ray before the candidate
            Optional<Vec3d> entHit = e.getBoundingBox().raycast(from, to);
            if (entHit.isPresent()) {
                double d2 = from.squaredDistanceTo(entHit.get());
                if (d2 < candidateDistSq - 1e-6) return true;
            }
        }
        return false;
    }
}
