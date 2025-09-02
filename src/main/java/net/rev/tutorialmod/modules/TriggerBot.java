package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.entity.Entity;
import net.minecraft.util.Hand;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.modules.filters.TargetFilters;

public class TriggerBot {

    public void onTick(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.triggerBotEnabled) {
            return;
        }

        HitResult crosshairTarget = client.crosshairTarget;
        if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.ENTITY) {
            Entity target = ((EntityHitResult) crosshairTarget).getEntity();

            if (TargetFilters.isValidTarget(target)) {
                if (client.player.getAttackCooldownProgress(0.5f) == 1.0f) {
                    if (client.interactionManager != null) {
                        client.interactionManager.attackEntity(client.player, target);
                        client.player.swingHand(Hand.MAIN_HAND);
                    }
                }
            }
        }
    }
}
