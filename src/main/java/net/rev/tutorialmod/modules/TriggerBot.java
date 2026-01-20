package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.modules.filters.TargetFilters;

import java.util.Random;

import net.minecraft.item.AxeItem;
import net.minecraft.item.MaceItem;
import net.minecraft.registry.tag.ItemTags;

public class TriggerBot {

    private Entity targetToAttack = null;
    private int reactionDelayTicks = 0;
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
                reactionDelayTicks = 0;
            }
            return; // Don't look for new targets while in delay
        }


        // --- Pre-computation checks ---
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.triggerBotEnabled || !TutorialMod.CONFIG.triggerBotToggledOn) {
            return;
        }

        // Hotkey Check
        try {
            String hotkey = TutorialMod.CONFIG.triggerBotHotkey;
            if (hotkey != null && !hotkey.equals("key.keyboard.unknown")) {
                long windowHandle = client.getWindow().getHandle();
                int keyCode = InputUtil.fromTranslationKey(hotkey).getCode();
                if (!InputUtil.isKeyPressed(windowHandle, keyCode)) {
                    // Hotkey is set and not pressed, so we do nothing.
                    targetToAttack = null;
                    attackDelayTicks = 0;
                    return;
                }
            }
        } catch (IllegalArgumentException e) {
            // Invalid key configured, just ignore and continue as if no hotkey was set.
        }

        // Check if a GUI is open
        if (client.currentScreen != null && !TutorialMod.CONFIG.triggerBotActiveInInventory) {
            return;
        }

        // Weapon Check
        if (TutorialMod.CONFIG.triggerBotWeaponOnly) {
            var stack = client.player.getMainHandStack();
            if (!stack.isIn(ItemTags.SWORDS) && !stack.isIn(ItemTags.AXES) && !(stack.getItem() instanceof MaceItem)) {
                reactionDelayTicks = 0;
                return;
            }
        }

        HitResult crosshairTarget = client.crosshairTarget;
        if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) crosshairTarget).getEntity();

            // --- Target Validation ---
            if (TargetFilters.isValidTarget(target)) {
                if (TutorialMod.CONFIG.attackOnCrit) {
                    if (client.player.getVelocity().y > 0) {
                        reactionDelayTicks = 0;
                        return;
                    }
                }
                // Range Check
                double distance = client.player.distanceTo(target);

                if (distance <= 4.5) {
                    // Cooldown Check
                    float charge = client.player.getAttackCooldownProgress(0.5f);
                    if (charge >= (float)(TutorialMod.CONFIG.triggerBotMinCharge / 100.0)) {

                        // Increment reaction timer
                        reactionDelayTicks++;

                        int minReaction = TutorialMod.CONFIG.triggerBotReactionMinDelay;
                        int maxReaction = TutorialMod.CONFIG.triggerBotReactionMaxDelay;
                        int requiredReaction = minReaction + (maxReaction > minReaction ? random.nextInt(maxReaction - minReaction + 1) : 0);

                        if (reactionDelayTicks >= requiredReaction) {
                            // Reaction time met, initiate attack sequence (post-cooldown delay)
                            int minAttack = TutorialMod.CONFIG.triggerBotAttackMinDelay;
                            int maxAttack = TutorialMod.CONFIG.triggerBotAttackMaxDelay;
                            this.attackDelayTicks = minAttack + (maxAttack > minAttack ? random.nextInt(maxAttack - minAttack + 1) : 0);
                            this.targetToAttack = target;

                            // If delay is 0, attack immediately
                            if (this.attackDelayTicks == 0) {
                                if (client.interactionManager != null) {
                                    client.interactionManager.attackEntity(client.player, targetToAttack);
                                    client.player.swingHand(Hand.MAIN_HAND);
                                    this.targetToAttack = null; // Reset
                                    reactionDelayTicks = 0;
                                }
                            }
                        }
                    } else {
                        // Not charged enough, but still looking at target?
                        // Keep reaction delay or reset? Typically reaction time starts when target is in crosshair regardless of charge.
                        // But user said: "time a valid entity needs to be intercepted by the crosshair(and within range) for the triggerbot to fire"
                        // I'll keep it simple and increment as long as we are looking.
                        reactionDelayTicks++;
                    }
                    return;
                }
            }
        }

        // If we reach here, we are not looking at a valid target in range
        reactionDelayTicks = 0;
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
        if (distance > 4.5) {
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
