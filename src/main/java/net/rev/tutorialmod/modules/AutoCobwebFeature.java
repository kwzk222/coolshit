package net.rev.tutorialmod.modules;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.SideShapeType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
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
import net.rev.tutorialmod.TutorialModClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Environment(EnvType.CLIENT)
public class AutoCobwebFeature {

    private static boolean isPlacing = false;
    private static int placementCooldown = 0;
    private static final int COOLDOWN_TICKS = 4; // 5 times a second

    public static float originalYaw;
    public static float originalPitch;

    public static void onTriggerPressed(MinecraftClient client) {
        if (client.player == null) return;
        isPlacing = true;
        placementCooldown = 0; // Place immediately on first press
        originalYaw = client.player.getYaw();
        originalPitch = client.player.getPitch();
    }

    public static void onTriggerReleased(MinecraftClient client) {
        isPlacing = false;
        if (client.player != null) {
            client.player.setYaw(originalYaw);
            client.player.setPitch(originalPitch);
        }
    }

    public static void onClientTick(MinecraftClient client) {
        if (!isPlacing || client.player == null || client.world == null) {
            return;
        }

        if (placementCooldown > 0) {
            placementCooldown--;
            return;
        }

        ClientPlayerEntity self = client.player;

        if (self.getMainHandStack().isEmpty() || self.getMainHandStack().getItem() != Items.COBWEB) {
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
            return;
        }

        Vec3d eye = self.getEyePos();
        Vec3d look = self.getRotationVector();
        PlayerEntity bestTarget = candidates.stream()
                .max(Comparator.comparingDouble(p -> p.getPos().subtract(eye).normalize().dotProduct(look)))
                .orElse(null);

        if (bestTarget == null) {
            return;
        }

        // --- New Targeting Logic ---
        Optional<BlockHitResult> hitResultOpt = Optional.empty();

        // Priority 1: Multi-Block Standing Positions
        List<BlockPos> standingBlocks = getBlocksUnderPlayer(bestTarget, client);
        for (BlockPos standingBlock : standingBlocks) {
            hitResultOpt = findVisibleHitOnBlock(client, self, bestTarget, standingBlock);
            if (hitResultOpt.isPresent()) {
                break; // Found a valid spot
            }
        }

        // Priority 2: Fallback
        if (hitResultOpt.isEmpty()) {
            BlockPos primaryTargetBlock = bestTarget.getBlockPos();
            List<BlockHitResult> validFallbacks = new ArrayList<>();

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) continue;

                    BlockPos candidateBlock = primaryTargetBlock.add(x, 0, z);
                    if (candidateBlock.equals(self.getBlockPos())) continue;

                    BlockState candidateState = client.world.getBlockState(candidateBlock);
                    if (candidateState.isSideSolid(client.world, candidateBlock, Direction.UP, SideShapeType.FULL) && client.world.getBlockState(candidateBlock.up()).isAir()) {
                        findVisibleHitOnBlock(client, self, bestTarget, candidateBlock).ifPresent(validFallbacks::add);
                    }
                }
            }

            if (!validFallbacks.isEmpty()) {
                // Find the one closest to the user
                hitResultOpt = validFallbacks.stream()
                        .min(Comparator.comparingDouble(h -> self.getEyePos().squaredDistanceTo(h.getPos())));
                hitResultOpt.ifPresent(h -> self.sendMessage(Text.literal("[AutoCobweb] Found fallback block: " + h.getBlockPos().toShortString()), false));
            }
        }


        if (hitResultOpt.isEmpty()) {
            return; // Failed to find any valid placement
        }

        placementCooldown = COOLDOWN_TICKS; // Reset cooldown only if we are attempting a placement

        BlockHitResult hitResult = hitResultOpt.get();
        Vec3d aim = hitResult.getPos();

        Vec3d d = aim.subtract(eye).normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(d.z, d.x)) - 90f;
        float pitch = (float) -Math.toDegrees(Math.asin(d.y));

        self.setYaw(yaw);
        self.setPitch(pitch);

        TutorialModClient.requestPlacement();
    }

    private static List<BlockPos> getBlocksUnderPlayer(PlayerEntity player, MinecraftClient client) {
        List<BlockPos> blocks = new ArrayList<>();
        Box bb = player.getBoundingBox();
        Box feetBox = new Box(bb.minX, bb.minY - 0.5, bb.minZ, bb.maxX, bb.minY, bb.maxZ);

        BlockPos min = BlockPos.ofFloored(feetBox.minX, feetBox.minY, feetBox.minZ);
        BlockPos max = BlockPos.ofFloored(feetBox.maxX, feetBox.maxY, feetBox.maxZ);

        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (client.world.getBlockState(pos).isSolid()) {
                blocks.add(pos.toImmutable());
            }
        }
        // If no solid blocks found directly under, just use the player's blockpos as a default
        if (blocks.isEmpty()) {
            blocks.add(player.getBlockPos());
        }
        return blocks;
    }

    private static Optional<BlockHitResult> findVisibleHitOnBlock(MinecraftClient client, ClientPlayerEntity self, PlayerEntity target, BlockPos block) {
        Vec3d eye = self.getEyePos();

        List<Vec3d> candidatePoints = new ArrayList<>();
        double[] offsets = {-0.35, 0.0, 0.35};
        for (double dx : offsets) {
            for (double dz : offsets) {
                candidatePoints.add(new Vec3d(block.getX() + 0.5 + dx, block.getY() + 0.999, block.getZ() + 0.5 + dz));
            }
        }

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

        return Optional.of(pickWeightedRandomPoint(eye, self.getRotationVector(), visibleHits));
    }

    private static boolean isBlockedByEntity(MinecraftClient client, Vec3d from, Vec3d to, PlayerEntity self, PlayerEntity target, double candidateDistSq) {
        if (client.world == null) return false;

        Box searchBox = new Box(from, to).expand(0.1);

        for (Entity e : client.world.getOtherEntities(self, searchBox)) {
            if (e.isSpectator()) continue;

            Optional<Vec3d> entHit = e.getBoundingBox().expand(0.1).raycast(from, to);
            if (entHit.isPresent()) {
                if (e == target) {
                    if (from.squaredDistanceTo(entHit.get()) < candidateDistSq) {
                        return true;
                    }
                } else {
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
}
