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
import net.rev.tutorialmod.TutorialModClient;
import net.rev.tutorialmod.modules.filters.TargetFilters;

public class AimAssist {
    private final MinecraftClient mc = MinecraftClient.getInstance();
    private long lastFrameTime = 0;
    private boolean isAssisting = false;
    private int failTicks = 0;
    private final java.util.Random random = new java.util.Random();

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

        // --- THE STRICTEST CHECK ---
        // If we are NOT already assisting, and our crosshair is ANYWHERE on ANY valid target's hitbox, we do nothing.
        // This completely prevents micro-tracking when the user is already on target.
        if (!isAssisting && isCrosshairOnAnyTarget()) {
            lastFrameTime = 0;
            return;
        }

        Entity target = findTarget();
        if (target == null) {
            isAssisting = false;
            lastFrameTime = 0;
            return;
        }

        // --- FAKE FAIL PREDICTION ---
        if (failTicks > 0) {
            failTicks--;
            lastFrameTime = 0;
            return;
        }

        if (TutorialMod.CONFIG.aimAssistFakeFailChance > 0 && random.nextInt(1000) < TutorialMod.CONFIG.aimAssistFakeFailChance) {
            failTicks = 5 + random.nextInt(10);
            isAssisting = false;
            lastFrameTime = 0;
            return;
        }

        // If we get here, we either were already assisting, or we were off-target.
        if (!isAssisting) {
            TutorialModClient.getInstance().setOverlayStatus("Aim Assist Active");
        }
        isAssisting = true;

        Vec3d targetPos = target.getBoundingBox().getCenter();

        // STOP condition: If we are close enough to the center (deadzone), stop assisting.
        // The trigger margin goes "in" from the center.
        if (isCrosshairOnPoint(targetPos, TutorialMod.CONFIG.aimAssistTriggerMargin)) {
            isAssisting = false;
            lastFrameTime = 0;
            return;
        }

        rotateToward(target, targetPos);
    }

    private boolean isCrosshairOnPoint(Vec3d targetPos, double margin) {
        if (mc.player == null) return false;
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d direction = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(TutorialMod.CONFIG.aimAssistMaxRange + 1.0));

        // Margin goes 'in' (smaller box around center)
        Box box = new Box(targetPos.x - margin, targetPos.y - margin, targetPos.z - margin,
                          targetPos.x + margin, targetPos.y + margin, targetPos.z + margin);
        return box.raycast(start, end).isPresent();
    }

    private boolean isHoldingMeleeWeapon() {
        if (mc.player == null) return false;
        return TutorialModClient.getInstance().isMeleeWeapon(mc.player.getMainHandStack());
    }

    private Entity findTarget() {
        if (mc.world == null || mc.player == null) return null;
        Entity closest = null;
        double minDist = TutorialMod.CONFIG.aimAssistMaxRange;

        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player || !entity.isAlive() || entity instanceof EndCrystalEntity) continue;
            if (!TargetFilters.isValidTarget(entity, true)) continue;

            double dist = mc.player.distanceTo(entity);
            if (dist < TutorialMod.CONFIG.aimAssistMinRange || dist > minDist) continue;

            if (!isInFov(entity, (float) TutorialMod.CONFIG.aimAssistFov)) continue;

            minDist = dist;
            closest = entity;
        }
        return closest;
    }

    private boolean isInFov(Entity entity, float fov) {
        if (mc.player == null) return false;
        Vec3d diff = entity.getBoundingBox().getCenter().subtract(mc.player.getCameraPosVec(1.0f));
        double yaw = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90.0;
        double pitch = -Math.toDegrees(Math.atan2(diff.y, Math.sqrt(diff.x * diff.x + diff.z * diff.z)));

        double yawDiff = Math.abs(MathHelper.wrapDegrees(yaw - mc.player.getYaw()));
        double pitchDiff = Math.abs(MathHelper.wrapDegrees(pitch - mc.player.getPitch()));

        // FOV is usually total width, so we check against fov/2
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

            Box box = entity.getBoundingBox().expand(entity.getTargetingMargin());
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

        double strength = TutorialMod.CONFIG.aimAssistStrength;

        if (TutorialMod.CONFIG.aimAssistVariableStrength) {
            double dist = mc.player.distanceTo(target);
            // Stronger when close, weaker when far? Or vice versa?
            // "track more if that slider is lower sensitivity and flick more if its higher"
            // Usually, we want the angular speed to feel consistent.
            // A simple distance scaling:
            double distFactor = 4.0 / Math.max(1.0, dist); // Higher factor when closer
            strength *= distFactor * TutorialMod.CONFIG.aimAssistVariableStrengthFactor;
        }

        // --- SENSITIVITY MATCH ---
        if (TutorialMod.CONFIG.aimAssistSensitivityMatch) {
            double sens = mc.options.getMouseSensitivity().getValue();
            // Sensitivity usually ranges from 0.0 to 1.0
            // We want the step to be proportional to how much the mouse usually moves.
            strength *= (sens * 2.0 + 0.1);
        }

        double step = strength * 8.0 * deltaTime;
        if (step > 1.0) step = 1.0;

        float newYaw = currentYaw + (float)(yawDiff * step);
        float newPitch = currentPitch + (float)(pitchDiff * step);

        mc.player.setYaw(newYaw);
        if (!TutorialMod.CONFIG.aimAssistHorizontalOnly) {
            mc.player.setPitch(newPitch);
        }
    }
}
