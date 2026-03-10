package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import net.rev.tutorialmod.modules.filters.TargetFilters;

import java.util.Random;

public class AimAssist {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private final Random random = new Random();
    private long lastFrameTime = 0;
    private boolean isAssisting = false;

    private Entity currentTarget = null;
    private Vec3d lastTargetPos = null;
    private Vec3d targetVelocity = Vec3d.ZERO;

    private double smoothYawStep = 0;
    private double smoothPitchStep = 0;
    private float currentSpeedScale = 0;

    public void onTick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialModClient.isKeyDown(TutorialMod.CONFIG.aimAssistHotkey)) {
            isAssisting = false;
        }
    }

    public void onRender(RenderTickCounter tickCounter) {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialModClient.isKeyDown(TutorialMod.CONFIG.aimAssistHotkey)) {
            reset();
            return;
        }

        if (mc.currentScreen != null) {
            reset();
            return;
        }

        if (TutorialMod.CONFIG.aimAssistWeaponOnly && !isHoldingMeleeWeapon()) {
            reset();
            return;
        }

        if (mc.player.getAttackCooldownProgress(0.0f) < TutorialMod.CONFIG.aimAssistChargeThreshold) {
            reset();
            return;
        }

        if (isCrosshairOnAnyTarget()) {
            isAssisting = false;
            lastFrameTime = 0;
            // We don't reset currentSpeedScale here to keep it ready if we drift off
            return;
        }

        Entity target = findTarget();
        if (target == null) {
            reset();
            currentTarget = null;
            lastTargetPos = null;
            return;
        }

        float tickDelta = tickCounter.getTickProgress(true);
        double tx = MathHelper.lerp(tickDelta, target.lastRenderX, target.getX());
        double ty = MathHelper.lerp(tickDelta, target.lastRenderY, target.getY());
        double tz = MathHelper.lerp(tickDelta, target.lastRenderZ, target.getZ());
        Vec3d targetPos = new Vec3d(tx, ty + target.getHeight() / 2.0, tz);

        if (target != currentTarget) {
            currentTarget = target;
            lastTargetPos = targetPos;
            targetVelocity = Vec3d.ZERO;
            currentSpeedScale = 0; // Reset acceleration for new target
        } else {
            targetVelocity = targetPos.subtract(lastTargetPos);
            lastTargetPos = targetPos;
        }

        if (!isAssisting) {
            TutorialModClient.getInstance().setOverlayStatus("Aim Assist Active");
        }
        isAssisting = true;

        rotateToward(target, targetPos);
    }

    private void reset() {
        isAssisting = false;
        lastFrameTime = 0;
        currentSpeedScale = 0;
        smoothYawStep = 0;
        smoothPitchStep = 0;
    }

    private boolean isHoldingMeleeWeapon() {
        if (mc.player == null) return false;
        ItemStack stack = mc.player.getMainHandStack();
        return TutorialModClient.getInstance().isMeleeWeapon(stack) || stack.getItem() instanceof net.minecraft.item.CrossbowItem;
    }

    private Entity findTarget() {
        if (mc.world == null || mc.player == null) return null;
        Entity closest = null;
        double minDist = TutorialMod.CONFIG.aimAssistMaxRange;
        boolean isShielding = mc.player.isUsingItem() && mc.player.getActiveItem().isOf(net.minecraft.item.Items.SHIELD);
        float fov = (float) (isShielding ? TutorialMod.CONFIG.aimAssistShieldFov : TutorialMod.CONFIG.aimAssistFov);

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive() || entity instanceof EndCrystalEntity) continue;
            if (!TargetFilters.isValidTarget(entity, true)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist < TutorialMod.CONFIG.aimAssistMinRange || dist > minDist) continue;

            if (!isInFov(entity, fov)) continue;

            if (isShielding) {
                if (isWithinShieldArc(entity)) continue;
            }

            minDist = dist;
            closest = entity;
        }
        return closest;
    }

    private boolean isWithinShieldArc(Entity target) {
        if (mc.player == null) return false;
        Vec3d diff = target.getBoundingBox().getCenter().subtract(mc.player.getCameraPosVec(1.0f));
        double targetYaw = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0;
        double yawDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - mc.player.getYaw()));

        double limit = TutorialMod.CONFIG.aimAssistShieldArc / 2.0;
        if (isAssisting) limit -= 5.0;

        return yawDiff <= limit;
    }

    private boolean isInFov(Entity entity, float fov) {
        if (mc.player == null) return false;
        Vec3d diff = entity.getBoundingBox().getCenter().subtract(mc.player.getCameraPosVec(1.0f));
        double yaw = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0;
        double pitch = -Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));

        double yawDiff = Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
        double pitchDiff = Math.abs(MathHelper.wrapDegrees(pitch - mc.player.getPitch()));

        return yawDiff <= fov / 2.0 && pitchDiff <= fov / 2.0;
    }

    private boolean isCrosshairOnAnyTarget() {
        if (mc.player == null || mc.world == null) return false;
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d direction = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(TutorialMod.CONFIG.aimAssistMaxRange + 1.0));

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive()) continue;
            if (!TargetFilters.isValidTarget(entity, true)) continue;

            Box box = entity.getBoundingBox();
            if (box.raycast(start, end).isPresent()) return true;
        }
        return false;
    }

    private void rotateToward(Entity target, Vec3d targetPos) {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        if (lastFrameTime == 0) {
            lastFrameTime = now;
            return;
        }
        float deltaTime = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;

        if (deltaTime > 0.1f) deltaTime = 0.1f;

        // --- PREDICTION ---
        Vec3d finalTargetPos = targetPos;
        if (TutorialMod.CONFIG.aimAssistPrediction) {
            double dist = mc.player.distanceTo(target);
            double predictTicks = dist * 2.0 * TutorialMod.CONFIG.aimAssistPredictionFactor;
            finalTargetPos = targetPos.add(targetVelocity.multiply(predictTicks));
        }

        Vec3d diff = finalTargetPos.subtract(mc.player.getCameraPosVec(1.0f));

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

        boolean isShielding = mc.player.isUsingItem() && mc.player.getActiveItem().isOf(net.minecraft.item.Items.SHIELD);
        double baseStrength = isShielding ? TutorialMod.CONFIG.aimAssistShieldStrength : TutorialMod.CONFIG.aimAssistStrength;

        // --- HUMAN-LIKE CURVE (Acceleration/Deceleration) ---
        double angleToTarget = Math.sqrt(yawDiff * yawDiff + pitchDiff * pitchDiff);

        // Acceleration
        float accelRate = (float) TutorialMod.CONFIG.aimAssistAcceleration * deltaTime * 5.0f;
        currentSpeedScale = Math.min(1.0f, currentSpeedScale + accelRate);

        // Deceleration (Ease out as we get closer to center)
        // Assume deceleration starts at 5 degrees
        double decelerationFactor = Math.min(1.0, angleToTarget / (5.0 * TutorialMod.CONFIG.aimAssistDeceleration));
        decelerationFactor = 0.1 + 0.9 * decelerationFactor; // Don't stop entirely, just slow down

        double strength = baseStrength * currentSpeedScale * decelerationFactor;

        double step = strength * 8.0 * deltaTime;
        if (step > 1.0) step = 1.0;

        double targetYawStep = yawDiff * step;
        double targetPitchStep = pitchDiff * step;

        // --- EMA SMOOTHING ---
        double emaAlpha = TutorialMod.CONFIG.aimAssistEmaAlpha;
        smoothYawStep = smoothYawStep * (1.0 - emaAlpha) + targetYawStep * emaAlpha;
        smoothPitchStep = smoothPitchStep * (1.0 - emaAlpha) + targetPitchStep * emaAlpha;

        float finalYawStep = (float)smoothYawStep;
        float finalPitchStep = (float)smoothPitchStep;

        float newYaw = currentYaw + finalYawStep;
        float newPitch = currentPitch + finalPitchStep;

        mc.player.setYaw(newYaw);
        if (!TutorialMod.CONFIG.aimAssistHorizontalOnly) {
            mc.player.setPitch(newPitch);
        }
    }
}
