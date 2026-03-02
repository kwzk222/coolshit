package net.rev.tutorialmod.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.rev.tutorialmod.TutorialMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "getBlockInteractionRange", at = @At("RETURN"), cancellable = true)
    private void onGetBlockInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (TutorialMod.CONFIG.creativeReachMatchSurvival) {
            PlayerEntity self = (PlayerEntity) (Object) this;
            if (self.isCreative()) {
                // In 1.21.1, Creative base is 5.0, Survival base is 4.5.
                // Subtract 0.5 from the total attribute value to match survival's scaling.
                cir.setReturnValue(cir.getReturnValue() - 0.5);
            }
        }
    }

    @Inject(method = "getEntityInteractionRange", at = @At("RETURN"), cancellable = true)
    private void onGetEntityInteractionRange(CallbackInfoReturnable<Double> cir) {
        if (TutorialMod.CONFIG.creativeReachMatchSurvival) {
            PlayerEntity self = (PlayerEntity) (Object) this;
            if (self.isCreative()) {
                // In 1.21.1, Creative base is 5.0, Survival base is 3.0.
                // Subtract 2.0 from the total attribute value to match survival's scaling.
                cir.setReturnValue(cir.getReturnValue() - 2.0);
            }
        }
    }
}
