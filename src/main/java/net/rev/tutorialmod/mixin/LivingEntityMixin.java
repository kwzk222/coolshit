package net.rev.tutorialmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "stopUsingItem", at = @At("HEAD"), cancellable = true)
    private void onStopUsingItem(CallbackInfo ci) {
        LivingEntity entity = (LivingEntity) (Object) this;

        // Only apply to local player
        if (entity != MinecraftClient.getInstance().player) return;

        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.bowReleaseBlockEnabled) return;

        ItemStack stack = entity.getActiveItem();
        if (stack.getItem() instanceof BowItem) {
            int useTicks = entity.getItemUseTime();
            float progress = BowItem.getPullProgress(useTicks);

            // If we are below full charge, and it's NOT an auto-release trigger, cancel the stop
            if (progress < 1.0f && !TutorialModClient.getInstance().isAutoReleasingBow()) {
                ci.cancel();
            } else {
                // If it's a full charge or auto-release, record the shot
                TutorialModClient.recordBowUsage();
            }
        }
    }
}
