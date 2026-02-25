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
        if (!(entity instanceof net.minecraft.client.network.ClientPlayerEntity clientPlayer)) return;

        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.bowReleaseBlockEnabled) return;

        ItemStack stack = clientPlayer.getActiveItem();
        if (stack.getItem() instanceof net.minecraft.item.BowItem) {
            int useTicks = clientPlayer.getItemUseTime();
            float progress = net.minecraft.item.BowItem.getPullProgress(useTicks);

            // Check if we are waiting for an auto-release OR if it's a full charge release
            if (TutorialModClient.getInstance().isAutoReleasingBow() || progress >= 1.0f) {
                TutorialModClient.recordBowUsage();
                return; // Allow
            }

            // If we are below full charge, and the use key is NOT pressed, block the stop
            if (!net.minecraft.client.MinecraftClient.getInstance().options.useKey.isPressed()) {
                ci.cancel();
            }
        }
    }
}
