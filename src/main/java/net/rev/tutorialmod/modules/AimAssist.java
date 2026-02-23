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

    // We don't use onTick for rotation anymore to ensure smoothness
    public void onTick() {
        // We can use onTick to reset state if needed
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

        boolean onTarget = isCrosshairOnTarget(target);

        if (!isAssisting) {
            if (onTarget) {
                // Already tracking, do nothing as requested
                lastFrameTime = 0;
                return;
            } else {
                // Off target, start assisting
                isAssisting = true;
            }
        }

        // If we are assisting, we continue until "center-ish"
        rotateToward(target);

        if (isCloseToCenter(target)) {
            isAssisting = false;
        }
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

    private boolean isCrosshairOnTarget(Entity target) {
        if (mc.player == null) return false;
        Vec3d start = mc.player.getCameraPosVec(1.0f);
        Vec3d direction = mc.player.getRotationVec(1.0f);
        Vec3d end = start.add(direction.multiply(TutorialMod.CONFIG.aimAssistMaxRange + 1.0));

        Box box = target.getBoundingBox().expand(target.getTargetingMargin() - TutorialMod.CONFIG.aimAssistTriggerMargin);
        return box.raycast(start, end).isPresent();
    }

    private void rotateToward(Entity target) {
        if (mc.player == null) return;

        long now = System.currentTimeMillis();
        if (lastFrameTime == 0) {
            lastFrameTime = now;
            return;
        }
        float deltaTime = (now - lastFrameTime) / 1000f;
        lastFrameTime = now;

        // Limit deltaTime to avoid huge jumps after lag spikes
        if (deltaTime > 0.1f) deltaTime = 0.1f;

        Vec3d targetPos = target.getBoundingBox().getCenter();

        // Randomization logic
        if (now - lastRandomTime > 500) {
            double m = TutorialMod.CONFIG.aimAssistCenterMargin;
            lastRandomOffset = new Vec3d(
                (random.nextDouble() - 0.5) * target.getWidth() * m,
                (random.nextDouble() - 0.5) * target.getHeight() * m,
                (random.nextDouble() - 0.5) * target.getWidth() * m
            );
            lastRandomTime = now;
        }
        targetPos = targetPos.add(lastRandomOffset);

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

        // Frame-rate independent smoothing
        // Strength * factor * deltaTime
        // At 1.0 strength, we want it to be reasonably fast but smooth.
        double step = TutorialMod.CONFIG.aimAssistStrength * 8.0 * deltaTime;
        if (step > 1.0) step = 1.0;

        float newYaw = currentYaw + (float)(yawDiff * step);
        float newPitch = currentPitch + (float)(pitchDiff * step);

        mc.player.setYaw(newYaw);
        if (!TutorialMod.CONFIG.aimAssistHorizontalOnly) {
            mc.player.setPitch(newPitch);
        }
    }

    private boolean isCloseToCenter(Entity target) {
        if (mc.player == null) return false;

        Vec3d targetPos = target.getBoundingBox().getCenter().add(lastRandomOffset);
        Vec3d diff = targetPos.subtract(mc.player.getCameraPosVec(1.0f));

        double diffX = diff.x;
        double diffY = diff.y;
        double diffZ = diff.z;
        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float targetYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0);
        float targetPitch = (float) MathHelper.wrapDegrees(-Math.toDegrees(Math.atan2(diffY, diffXZ)));

        float yawDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - mc.player.getYaw()));
        float pitchDiff = Math.abs(MathHelper.wrapDegrees(targetPitch - mc.player.getPitch()));

        // Consider "center-ish" as within 1 degree
        if (TutorialMod.CONFIG.aimAssistHorizontalOnly) {
            return yawDiff < 1.0f;
        } else {
            return yawDiff < 1.0f && pitchDiff < 1.0f;
        }
    }
}
