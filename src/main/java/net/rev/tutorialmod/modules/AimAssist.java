package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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

    public void onTick() {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.aimAssistEnabled) {
            return;
        }

        if (mc.currentScreen != null) {
            return;
        }

        if (TutorialMod.CONFIG.aimAssistWeaponOnly && !isHoldingMeleeWeapon()) {
            return;
        }

        if (mc.player.getAttackCooldownProgress(0.0f) < TutorialMod.CONFIG.aimAssistChargeThreshold) {
            return;
        }

        Entity target = findTarget();
        if (target == null) {
            return;
        }

        if (isCrosshairOnTarget(target)) {
            return;
        }

        rotateToward(target);
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

        Box box = target.getBoundingBox().expand(target.getTargetingMargin() + TutorialMod.CONFIG.aimAssistTriggerMargin);
        return box.raycast(start, end).isPresent();
    }

    private void rotateToward(Entity target) {
        if (mc.player == null) return;
        Vec3d targetPos = target.getBoundingBox().getCenter();

        // Randomization logic
        if (System.currentTimeMillis() - lastRandomTime > 500) {
            double m = TutorialMod.CONFIG.aimAssistCenterMargin;
            lastRandomOffset = new Vec3d(
                (random.nextDouble() - 0.5) * target.getWidth() * m,
                (random.nextDouble() - 0.5) * target.getHeight() * m,
                (random.nextDouble() - 0.5) * target.getWidth() * m
            );
            lastRandomTime = System.currentTimeMillis();
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

        // Assist Strength as a speed multiplier. 1.0 = standard speed.
        // We multiply by a small factor to make it feel smooth at 1.0
        double strength = TutorialMod.CONFIG.aimAssistStrength * 0.2;

        float newYaw = currentYaw + (float)(yawDiff * strength);
        float newPitch = currentPitch + (float)(pitchDiff * strength);

        mc.player.setYaw(newYaw);
        if (!TutorialMod.CONFIG.aimAssistHorizontalOnly) {
            mc.player.setPitch(newPitch);
        }
    }
}
