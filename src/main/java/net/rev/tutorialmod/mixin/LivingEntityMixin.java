package net.rev.tutorialmod.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Shadow private int jumpingCooldown;

    @Shadow public abstract ItemStack getActiveItem();

    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        if (TutorialMod.CONFIG.autoJumpEnabled && (Object)this instanceof ClientPlayerEntity) {
            if (TutorialModClient.getInstance().isBuildingRecently()) {
                this.jumpingCooldown = 0;
            }
        }
    }

    @Inject(method = "stopUsingItem", at = @At("HEAD"))
    private void onStopUsingItem(CallbackInfo ci) {
        if (!TutorialMod.CONFIG.masterEnabled) return;

        if ((Object)this instanceof ClientPlayerEntity player) {
            ItemStack stack = this.getActiveItem();
            if (stack.getItem() instanceof CrossbowItem) {
                // For crossbows, record usage when they are actually charged and then used
                // The bowReleaseBlock handles bows, but crossbows fire on right-click when charged
                // Wait, recordBowUsage is also called in InteractionManager for interactItem
            }
        }
    }
}
