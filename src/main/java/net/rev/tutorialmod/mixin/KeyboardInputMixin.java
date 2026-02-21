package net.rev.tutorialmod.mixin;

import net.minecraft.client.input.KeyboardInput;
import net.rev.tutorialmod.TutorialModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        TutorialModClient instance = TutorialModClient.getInstance();
        if (instance != null) {
            instance.handleSprintResetInput((KeyboardInput)(Object)this);
        }
    }
}
