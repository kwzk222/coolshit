package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.modules.filters.TargetFilters;

import java.util.Random;

public class AimAssist {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();
    private Vec3d lastRandomOffset = Vec3d.ZERO;
    private long lastRandomTime = 0;
    private long lastFrameTime = 0;
    private boolean isAssisting = false;

    public void onTick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.aimAssistEnabled) {
            isAssisting = false;
        }
    }

    public void onRender(RenderTickCounter tickCounter) {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.aimAssistEnabled) {
            isAssisting = false;
            lastFrameTime = 0;
            return;
        }

        if (mc.currentScreen != null) {
            isAssisting = false;
            lastFrameTime = 0;
            return;
        }

        if (TutorialMod.CONFIG.aimAssistWeaponOnly && !isHoldingMeleeWeapon()) {
            isAssisting = false;
            lastFrameTime = 0;
            return;
        }

        if (mc.player.getAttackCooldownProgress(0.0f) < TutorialMod.CONFIG.aimAssistChargeThreshold) {
            isAssisting = false;
            lastFrameTime = 0;
            return;
        }

        Entity target = findTarget();
        if (target == null) {
            isAssisting = false;
            lastFrameTime = 0;
            return;
        }

        Vec3d targetPos = getTargetPos(target);

        // --- Smart Triggering Logic ---

        // 1. If not assisting, we ONLY start if we are completely off the target's REAL hitbox.
        // This avoids "microtracking" jitter while already tracking.
        if (!isAssisting) {
            if (isCrosshairOnTarget(target)) {
                lastFrameTime = 0;
                return;
            }
            isAssisting = true;
        }

        // 2. If assisting, we continue until we hit the "center-ish" zone.
        // The trigger margin defines the size of this stop zone.
        if (isCrosshairOnPoint(targetPos, TutorialMod.CONFIG.aimAssistTriggerMargin)) {
            isAssisting = false;
            lastFrameTime = 0;
            return;
        }

        // 3. Move toward target
        rotateToward(targetPos);
    }

    private boolean isHoldingMeleeWeapon() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandStack();
        return stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES) || stack.getItem() instanceof MaceItem || stack.isIn(ItemTags.SPEARS);
    }

    private Entity findTarget() {
        if (mc.world == null || mc.player == null) return null;
        Entity closest = null;
        double minDist = TutorialMod.CONFIG.aimAssistMaxRange;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive() || entity instanceof EndCrystalEntity) continue;
            if (!TargetFilters.isValidTarget(entity)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist >= TutorialMod.CONFIG.aimAssistMinRange && dist <= minDist) {
                minDist = dist;
                closest = entity;
            }
        }
        return closest;
    }

    private Vec3d getTargetPos(Entity target) {
        long now = System.currentTimeMillis();
        if (now - lastRandomTime > 500) {
            double m = TutorialMod.CONFIG.aimAssistCenterMargin;
            lastRandomOffset = new Vec3d(
                (random.nextDouble() - 0.5) * target.getWidth() * m,
                (random.nextDouble() - 0.5) * target.getHeight() * m,
                (random.nextDouble() - 0.5) * target.getWidth() * m
            );
            lastRandomTime = now;
        }
        return target.getBoundingBox().getCenter().add(lastRandomOffset);
    }

    private boolean isCrosshairOnTarget(Entity target) {
        if (mc.player == null) return false;
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d direction = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(TutorialMod.CONFIG.aimAssistMaxRange + 1.0));

        Box box = target.getBoundingBox().expand(target.getTargetingMargin());
        return box.raycast(start, end).isPresent();
    }

    private boolean isCrosshairOnPoint(Vec3d point, double radius) {
        if (mc.player == null) return false;
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d direction = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(TutorialMod.CONFIG.aimAssistMaxRange + 1.0));

        double r = Math.max(0.005, radius);
        Box box = new Box(point.x - r, point.y - r, point.z - r, point.x + r, point.y + r, point.z + r);
        return box.raycast(start, end).isPresent();
    }

    private void rotateToward(Vec3d targetPos) {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        if (lastFrameTime == 0) {
            lastFrameTime = now;
            return;
        }
        float deltaTime = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;

        if (deltaTime > 0.1f) deltaTime = 0.1f;

        Vec3d diff = targetPos.subtract(mc.player.getCameraPosVec(1.0f));

        double diffX = diff.x;
        double diffY = diff.y;
        double diffZ = diff.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float targetPitch = (float) MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(diffY, diffXZ)));

        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float pitchDiff = MathHelper.wrapDegrees(targetPitch - currentPitch);

        double step = TutorialMod.CONFIG.aimAssistStrength * 8.0 * deltaTime;
        if (step > 1.0) step = 1.0;

        float newYaw = currentYaw + (float)(yawDiff * step);
        float newPitch = currentPitch + (float)(pitchDiff * step);

        mc.player.setYaw(newYaw);
        if (!TutorialMod.CONFIG.aimAssistHorizontalOnly) {
            mc.player.setPitch(newPitch);
        }
    }
}
