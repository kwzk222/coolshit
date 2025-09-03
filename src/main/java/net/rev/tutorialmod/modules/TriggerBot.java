package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.modules.filters.TargetFilters;

import java.util.Random;

public class TriggerBot {

    private Entity targetToAttack = null;
    private int attackDelayTicks = 0;
    private final Random random = new Random();

    public void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            // Clean up state if player leaves world
            targetToAttack = null;
            attackDelayTicks = 0;
            return;
        }

        // --- Handle Attack Delay ---
        if (attackDelayTicks > 0) {
            attackDelayTicks--;
            if (attackDelayTicks == 0 && targetToAttack != null) {
                // Verify target is still valid before attacking
                if (isTargetStillValid(client, targetToAttack)) {
                    if (client.interactionManager != null) {
                        client.interactionManager.attackEntity(client.player, targetToAttack);
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                }
                targetToAttack = null; // Reset target after action
            }
            return; // Don't look for new targets while in delay
        }


        // --- Pre-computation checks ---
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.triggerBotEnabled) {
            return;
        }

        // Check if a GUI is open
        if (client.currentScreen != null && !TutorialMod.CONFIG.triggerBotActiveInInventory) {
            return;
        }

        // We are not in an attack delay, so we can look for a new target.
        targetToAttack = null;

        HitResult crosshairTarget = client.crosshairTarget;
        if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) crosshairTarget).getEntity();

            // --- Target Validation ---
            if (TargetFilters.isValidTarget(target)) {
                // Range Check
                double distance = client.player.distanceTo(target);
                double maxRange = TutorialMod.CONFIG.triggerBotMaxRange;

                if (distance <= maxRange) {
                    // Cooldown Check
                    if (client.player.getAttackCooldownProgress(0.5f) == 1.0f) {
                        // Initiate attack sequence
                        int maxDelay = TutorialMod.CONFIG.triggerBotAttackDelay;
                        this.attackDelayTicks = (maxDelay > 0) ? random.nextInt(maxDelay + 1) : 0;
                        this.targetToAttack = target;

                        // If delay is 0, attack immediately
                        if (this.attackDelayTicks == 0) {
                             if (client.interactionManager != null) {
                                client.interactionManager.attackEntity(client.player, targetToAttack);
                                client.player.swingHand(Hand.MAIN_HAND);
                                this.targetToAttack = null; // Reset
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean isTargetStillValid(MinecraftClient client, Entity target) {
        if (target == null || !target.isAlive() || client.player == null) {
            return false;
        }
        // Re-run the same checks as before the delay
        if (!TargetFilters.isValidTarget(target)) {
            return false;
        }
        double distance = client.player.distanceTo(target);
        // We don't re-randomize range here, just check against the absolute max.
        // This prevents the target from becoming invalid just due to a new random roll.
        if (distance > TutorialMod.CONFIG.triggerBotMaxRange) {
            return false;
        }
        // Make sure we are still looking at the target
        HitResult crosshairTarget = client.crosshairTarget;
        if (crosshairTarget == null || crosshairTarget.getType() != HitResult.Type.ENTITY || !((EntityHitResult) crosshairTarget).getEntity().equals(target)) {
            return false;
        }

        return true;
    }
}
