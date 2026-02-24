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

public class AimAssist {
    private final MinecraftClient mc = MinecraftClient.getInstance();
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

        // --- THE STRICTEST CHECK ---
        // If crosshair is ANYWHERE on the target's hitbox, we do nothing.
        // This completely prevents micro-tracking.
        if (isCrosshairOnTarget(target)) {
            isAssisting = false;
            lastFrameTime = 0;
            return;
        }

        // If we are here, we are NOT on the target.
        // Start or continue assisting.
        isAssisting = true;

        Vec3d targetPos = target.getBoundingBox().getCenter();
        rotateToward(target, targetPos);
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

    private boolean isCrosshairOnTarget(Entity target) {
        if (mc.player == null) return false;
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d direction = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(TutorialMod.CONFIG.aimAssistMaxRange + 1.0));

        Box box = target.getBoundingBox().expand(target.getTargetingMargin());
        return box.raycast(start, end).isPresent();
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
