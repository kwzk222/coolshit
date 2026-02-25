package net.rev.tutorialmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow public ClientPlayerEntity player;
    private boolean lastUsePressed = false;

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void onHandleInputEvents(CallbackInfo ci) {
        MinecraftClient client = (MinecraftClient) (Object) this;
        if (player == null || !TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.bowReleaseBlockEnabled) return;

        // Check if useKey was just released
        boolean isUsePressed = client.options.useKey.isPressed();

        if (!isUsePressed && lastUsePressed) {
            ItemStack activeStack = player.getActiveItem();
            if (activeStack.getItem() instanceof BowItem) {
                int useTicks = player.getItemUseTime();
                float progress = BowItem.getPullProgress(useTicks);
                if (progress < 1.0f) {
                    TutorialModClient.getInstance().setPendingBowRelease(true);
                }
            }
        }
        lastUsePressed = isUsePressed;
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (TutorialModClient.getInstance().onReachSwap()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        if (TutorialModClient.getInstance().onItemUse()) {
            ci.cancel();
        }
    }
}
