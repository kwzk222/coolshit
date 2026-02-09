package net.rev.tutorialmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.GameOptions;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    @Shadow
    public GameOptions options;

    @Inject(method = "handleInputEvents", at = @At("TAIL"))
    private void onHandleInputEvents(CallbackInfo ci) {
        if (!this.options.attackKey.isPressed()) {
            TutorialMod.getAutoToolSwitch().onStoppedMining();
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"))
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (TutorialModClient.getInstance() != null) {
            TutorialModClient.getInstance().onReachSwap();
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        if (TutorialModClient.getInstance() != null && TutorialModClient.getInstance().onItemUse()) {
            ci.cancel();
        }
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen == null && TutorialModClient.getInstance() != null && TutorialModClient.getInstance().getESPModule() != null) {
            TutorialModClient.getInstance().getESPModule().syncWindowBounds();
        }
    }

    @Inject(method = "onWindowFocusChanged", at = @At("RETURN"))
    private void onWindowFocusChanged(boolean focused, CallbackInfo ci) {
        if (focused && TutorialModClient.getInstance() != null && TutorialModClient.getInstance().getESPModule() != null) {
            TutorialModClient.getInstance().getESPModule().syncWindowBounds();
        }
    }
}
