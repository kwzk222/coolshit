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
        if (!(entity instanceof net.minecraft.client.network.ClientPlayerEntity)) return;

        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.bowReleaseBlockEnabled) return;

        ItemStack stack = entity.getActiveItem();
        if (stack.getItem() instanceof net.minecraft.item.BowItem) {
            // Check if we are waiting for an auto-release
            if (TutorialModClient.getInstance().isAutoReleasingBow()) {
                TutorialModClient.recordBowUsage();
                return; // Allow
            }

            int useTicks = entity.getItemUseTime();
            float progress = net.minecraft.item.BowItem.getPullProgress(useTicks);

            // If we are below full charge, and the use key is NOT pressed, block the stop
            // This happens when the user releases the key early.
            // But we should only block if we are actually intending to auto-fire.
            if (progress < 1.0f && !net.minecraft.client.MinecraftClient.getInstance().options.useKey.isPressed()) {
                ci.cancel();
            } else if (progress >= 1.0f) {
                TutorialModClient.recordBowUsage();
            }
        }
    }
}
