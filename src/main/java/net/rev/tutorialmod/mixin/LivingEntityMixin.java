package net.rev.tutorialmod.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.rev.tutorialmod.TutorialMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow private int jumpingCooldown;

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        if (TutorialMod.CONFIG.autoJumpEnabled && (Object)this instanceof ClientPlayerEntity) {
            this.jumpingCooldown = 0;
        }
    }
}
