package net.rev.tutorialmod.modules.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TrajectoriesModule {
    private final MinecraftClient client = MinecraftClient.getInstance();

    public void onRender(Matrix4f combinedMatrix) {
        if (!TutorialMod.CONFIG.trajectoriesEnabled || client.player == null || client.world == null) return;

        ItemStack stack = client.player.getMainHandStack();
        ItemStack offStack = client.player.getOffHandStack();

        boolean isBow = stack.getItem() instanceof BowItem || offStack.getItem() instanceof BowItem;
        boolean isCrossbow = stack.getItem() instanceof CrossbowItem || offStack.getItem() instanceof CrossbowItem;
        boolean isPearl = stack.isOf(Items.ENDER_PEARL) || offStack.isOf(Items.ENDER_PEARL);
        boolean isRod = stack.isOf(Items.FISHING_ROD) || offStack.isOf(Items.FISHING_ROD);

        if (!isBow && !isCrossbow && !isPearl && !isRod) return;

        // Determine if active
        boolean active = false;
        float speed = 0f;
        float gravity = 0.05f;
        float drag = 0.99f;

        if (isBow) {
            ItemStack bow = stack.getItem() instanceof BowItem ? stack : offStack;
            int useTicks = client.player.getItemUseTime();
            if (client.player.isUsingItem() && client.player.getActiveItem() == bow) {
                float pull = BowItem.getPullProgress(useTicks);
                speed = pull * 3.0f;
                active = true;
            }
        } else if (isCrossbow) {
            ItemStack cb = stack.getItem() instanceof CrossbowItem ? stack : offStack;
            if (CrossbowItem.isCharged(cb)) {
                speed = 3.15f; // Crossbow arrow speed
                active = true;
            }
        } else if (isPearl) {
            speed = 1.5f;
            gravity = 0.03f;
            active = true;
        } else if (isRod) {
            if (client.player.fishHook == null) {
                speed = 1.1f; // Approximated
                gravity = 0.03f;
                drag = 0.92f;
                active = true;
            } else {
                handleRodPull(combinedMatrix);
            }
        }

        if (!active || speed <= 0.1f) return;

        simulate(speed, gravity, drag, combinedMatrix);
    }

    private void simulate(float speed, float gravity, float drag, Matrix4f combinedMatrix) {
        Vec3d pos = client.gameRenderer.getCamera().getCameraPos();
        Vec3d look = client.player.getRotationVec(1.0f);

        // Initial velocity
        Vec3d velocity = look.multiply(speed);

        // Inherit player velocity (simplified)
        Vec3d playerVel = client.player.getVelocity();
        velocity = velocity.add(playerVel.x, client.player.isOnGround() ? 0 : playerVel.y, playerVel.z);

        List<Vec3d> path = new ArrayList<>();
        path.add(pos);

        boolean hitEntity = false;
        int color = TutorialMod.CONFIG.trajectoriesColor;

        Vec3d currentPos = pos;
        Vec3d currentVel = velocity;
        BlockHitResult finalBlockHit = null;

        for (int i = 0; i < 100; i++) { // Max 100 ticks
            Vec3d nextPos = currentPos.add(currentVel);

            // Block collision
            BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                    currentPos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    client.player
            ));

            // Entity collision
            EntityHitResult entityHit = getEntityHit(currentPos, nextPos);

            if (entityHit != null) {
                path.add(entityHit.getPos());
                hitEntity = true;
                break;
            }

            if (blockHit.getType() != HitResult.Type.MISS) {
                path.add(blockHit.getPos());
                finalBlockHit = blockHit;
                break;
            }

            path.add(nextPos);
            currentPos = nextPos;
            currentVel = currentVel.multiply(drag).subtract(0, gravity, 0);
        }

        if (hitEntity) {
            color = TutorialMod.CONFIG.trajectoriesHitColor;
        }

        renderPath(path, color, combinedMatrix, finalBlockHit);
    }

    private void handleRodPull(Matrix4f combinedMatrix) {
        net.minecraft.entity.projectile.FishingBobberEntity bobber = client.player.fishHook;
        if (bobber == null || bobber.isRemoved()) return;

        Entity hooked = bobber.getHookedEntity();
        if (hooked == null || !hooked.isAlive()) return;

        // Calculate pull velocity
        // In FishingBobberEntity.use():
        // double d = this.getOwner().getX() - this.getX();
        // double e = this.getOwner().getY() - this.getY();
        // double f = this.getOwner().getZ() - this.getZ();
        // this.hookedEntity.setVelocity(this.hookedEntity.getVelocity().add(d * 0.1D, e * 0.1D + (double)MathHelper.sqrt((float)(d * d + e * e + f * f)) * 0.08D, f * 0.1D));

        Vec3d ownerPos = new Vec3d(client.player.getX(), client.player.getY(), client.player.getZ());
        Vec3d bobberPos = new Vec3d(bobber.getX(), bobber.getY(), bobber.getZ());
        Vec3d diff = ownerPos.subtract(bobberPos);

        double pullX = diff.x * 0.1;
        double pullY = diff.y * 0.1 + Math.sqrt(diff.x * diff.x + diff.y * diff.y + diff.z * diff.z) * 0.08;
        double pullZ = diff.z * 0.1;

        Vec3d startPos = new Vec3d(hooked.getX(), hooked.getY(), hooked.getZ());
        Vec3d startVel = hooked.getVelocity().add(pullX, pullY, pullZ);

        float gravity = 0.08f;
        float drag = 0.91f;

        // Adjust physics based on entity type
        if (hooked instanceof net.minecraft.entity.ItemEntity || hooked instanceof net.minecraft.entity.TntEntity) {
            gravity = 0.04f;
            drag = 0.98f;
        }

        simulateEntity(startPos, startVel, gravity, drag, combinedMatrix);
    }

    private void simulateEntity(Vec3d pos, Vec3d velocity, float gravity, float drag, Matrix4f combinedMatrix) {
        List<Vec3d> path = new ArrayList<>();
        path.add(pos);

        Vec3d currentPos = pos;
        Vec3d currentVel = velocity;
        BlockHitResult finalHit = null;

        for (int i = 0; i < 100; i++) {
            Vec3d nextPos = currentPos.add(currentVel);

            BlockHitResult blockHit = client.world.raycast(new RaycastContext(
                    currentPos, nextPos,
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE,
                    (net.minecraft.entity.Entity) null // Don't ignore anyone for this simulation
            ));

            if (blockHit.getType() != HitResult.Type.MISS) {
                path.add(blockHit.getPos());
                finalHit = blockHit;
                break;
            }

            path.add(nextPos);
            currentPos = nextPos;
            currentVel = currentVel.multiply(drag).subtract(0, gravity, 0);
        }

        renderPath(path, 0x00FF00, combinedMatrix, finalHit); // Green for pull prediction
    }

    private EntityHitResult getEntityHit(Vec3d start, Vec3d end) {
        Entity closestEntity = null;
        Vec3d hitPos = null;
        double minDistance = start.distanceTo(end);

        Box box = new Box(start, end).expand(1.0);
        for (Entity entity : client.world.getOtherEntities(client.player, box, e -> !e.isSpectator() && e.isAlive() && e.isAttackable())) {
            Box entityBox = entity.getBoundingBox().expand(entity.getTargetingMargin());
            java.util.Optional<Vec3d> hit = entityBox.raycast(start, end);
            if (hit.isPresent()) {
                double dist = start.distanceTo(hit.get());
                if (dist < minDistance) {
                    minDistance = dist;
                    closestEntity = entity;
                    hitPos = hit.get();
                }
            }
        }
        return closestEntity != null ? new EntityHitResult(closestEntity, hitPos) : null;
    }

    private void renderPath(List<Vec3d> path, int color, Matrix4f combinedMatrix, BlockHitResult blockHit) {
        if (path.size() < 2) return;

        StringBuilder sb = new StringBuilder();
        Vec3d camPos = client.gameRenderer.getCamera().getCameraPos();

        for (int i = 0; i < path.size(); i++) {
            Vec3d p = path.get(i);
            Vec3d rel = p.subtract(camPos);
            Vector4f v = new Vector4f((float)rel.x, (float)rel.y, (float)rel.z, 1.0f);
            combinedMatrix.transform(v);

            if (v.w > 0.01f) {
                float fovScale = TutorialMod.CONFIG.espManualProjection ? (float)TutorialMod.CONFIG.espFovScale : 1.0f;
                float aspectScale = TutorialMod.CONFIG.espManualProjection ? (float)TutorialMod.CONFIG.espAspectRatioScale : 1.0f;

                float x = ((v.x / v.w) * fovScale * aspectScale + 1.0f) * 0.5f;
                float y = (1.0f - (v.y / v.w) * fovScale) * 0.5f;

                // Visual offset to the right for the beginning of the line
                float progress = (float) i / (path.size() - 1);
                float offset = 0.05f * (1.0f - progress);
                x += offset;

                if (sb.length() > 0) sb.append(",");
                sb.append(String.format(Locale.ROOT, "%.4f,%.4f", x, y));
            }
        }

        if (sb.length() > 0) {
            TutorialModClient.getESPOverlayManager().sendCommand(String.format(Locale.ROOT, "TRAJECTORY %s|%d", sb.toString(), color));
        }

        if (blockHit != null && blockHit.getType() == HitResult.Type.BLOCK) {
            renderImpactPlane(blockHit, color, combinedMatrix);
        }
    }

    private void renderImpactPlane(BlockHitResult hit, int color, Matrix4f combinedMatrix) {
        Vec3d pos = hit.getPos();
        net.minecraft.util.math.Direction side = hit.getSide();
        Vec3d camPos = client.gameRenderer.getCamera().getCameraPos();

        // Calculate 4 corners of a 0.4x0.4 square
        Vec3d v1, v2;
        if (side.getAxis() == net.minecraft.util.math.Direction.Axis.Y) {
            v1 = new Vec3d(1, 0, 0);
            v2 = new Vec3d(0, 0, 1);
        } else if (side.getAxis() == net.minecraft.util.math.Direction.Axis.X) {
            v1 = new Vec3d(0, 1, 0);
            v2 = new Vec3d(0, 0, 1);
        } else {
            v1 = new Vec3d(1, 0, 0);
            v2 = new Vec3d(0, 1, 0);
        }

        double s = 0.2;
        Vec3d[] corners = {
                pos.add(v1.multiply(s)).add(v2.multiply(s)),
                pos.add(v1.multiply(s)).subtract(v2.multiply(s)),
                pos.subtract(v1.multiply(s)).subtract(v2.multiply(s)),
                pos.subtract(v1.multiply(s)).add(v2.multiply(s))
        };

        StringBuilder sb = new StringBuilder();
        for (Vec3d c : corners) {
            Vec3d rel = c.subtract(camPos);
            Vector4f v = new Vector4f((float)rel.x, (float)rel.y, (float)rel.z, 1.0f);
            combinedMatrix.transform(v);

            if (v.w > 0.01f) {
                float fovScale = TutorialMod.CONFIG.espManualProjection ? (float)TutorialMod.CONFIG.espFovScale : 1.0f;
                float aspectScale = TutorialMod.CONFIG.espManualProjection ? (float)TutorialMod.CONFIG.espAspectRatioScale : 1.0f;
                float x = ((v.x / v.w) * fovScale * aspectScale + 1.0f) * 0.5f;
                float y = (1.0f - (v.y / v.w) * fovScale) * 0.5f;
                if (sb.length() > 0) sb.append(",");
                sb.append(String.format(Locale.ROOT, "%.4f,%.4f", x, y));
            }
        }

        if (sb.length() > 0) {
            TutorialModClient.getESPOverlayManager().sendCommand(String.format(Locale.ROOT, "IMPACT_PLANE %s|%d", sb.toString(), color));
        }
    }
}
