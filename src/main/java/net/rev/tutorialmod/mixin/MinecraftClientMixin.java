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

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        if (TutorialModClient.getInstance().onLungeSwap()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void onDoItemUse(CallbackInfo ci) {
        if (TutorialModClient.getInstance().onItemUse()) {
            ci.cancel();
        }
    }

    @Inject(method = "handleInputEvents", at = @At("HEAD"))
    private void onHandleInputEvents(CallbackInfo ci) {
        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.hotbarHoldCombat) return;
        TutorialModClient instance = TutorialModClient.getInstance();
        if (instance != null && player != null) {
            long now = System.currentTimeMillis();
            for (int i = 0; i < 9; i++) {
                // If the key is held longer than 200ms, it's a combat activation.
                // We consume the 'wasPressed' state so the vanilla code doesn't switch slots AGAIN.
                if (instance.getHotbarKeyHoldStartTime(i) != 0 && (now - instance.getHotbarKeyHoldStartTime(i) > 200)) {
                    ItemStack stack = player.getInventory().getStack(i);
                    if (instance.isMeleeWeapon(stack)) {
                        while (((MinecraftClient)(Object)this).options.hotbarKeys[i].wasPressed()) {
                            // Consume the events
                        }
                    }
                }
            }
        }
    }
}
