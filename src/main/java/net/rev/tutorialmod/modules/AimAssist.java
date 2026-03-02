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
        // If our crosshair is ANYWHERE on ANY valid target's hitbox, we do nothing.
        // This completely prevents tracking when the user is already on target.
        if (isCrosshairOnAnyTarget()) {
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

        // If we get here, we were off-target.
        if (!isAssisting) {
            TutorialModClient.getInstance().setOverlayStatus("Aim Assist Active");
        }
        isAssisting = true;

        Vec3d targetPos = target.getBoundingBox().getCenter();

        rotateToward(target, targetPos);
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

        // Add a 5-degree buffer to prevent jitter when target is on the edge
        double arc = TutorialMod.CONFIG.aimAssistShieldArc;
        if (isAssisting) arc += 10.0; // wider arc once assisting to keep it engaged?
        // Actually the requirement is "follow enough to protect".
        // If we are already assisting, we want to stay assisting until they are comfortably inside the arc.
        // So arc should be SMALLER when isAssisting is true?
        // Wait, if they are outside 180, we assist. We stop when they are inside 180.
        // To prevent jitter, we should start assisting at 180 and stop at say 170.

        double limit = TutorialMod.CONFIG.aimAssistShieldArc / 2.0;
        if (isAssisting) limit -= 5.0; // Must get closer to center to stop assisting

        return yawDiff <= limit;
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

            // Margin goes "in" -> negative expansion
            Box box = entity.getBoundingBox().expand(-0.05);
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

        boolean isShielding = mc.player.isUsingItem() && mc.player.getActiveItem().isOf(net.minecraft.item.Items.SHIELD);
        double strength = isShielding ? TutorialMod.CONFIG.aimAssistShieldStrength : TutorialMod.CONFIG.aimAssistStrength;

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
