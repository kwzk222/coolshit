package net.rev.tutorialmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.rev.tutorialmod.TutorialModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BowItem.class)
public class BowItemMixin {
    @Inject(method = "onStoppedUsing", at = @At("HEAD"), cancellable = true)
    private void onStoppedUsingHead(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<ItemStack> info) {
        if (world.isClient() && user instanceof net.minecraft.entity.player.PlayerEntity player) {
            if (net.rev.tutorialmod.TutorialMod.CONFIG.bowReleaseBlockEnabled) {
                int useTicks = stack.getMaxUseTime(user) - remainingUseTicks;
                // Minecraft BowItem.onStoppedUsing requires pullProgress >= 0.1 to fire.
                // pullProgress = getPullProgress(useTicks) = useTicks / 20.0.
                // So 0.1 * 20 = 2 ticks.
                // We block if less than 3 ticks to ensure it doesn't fail "silently" and waste the click.
                if (useTicks < 3) {
                    info.setReturnValue(stack);
                    info.cancel();
                }
            }
        }
    }

    @Inject(method = "onStoppedUsing", at = @At("TAIL"))
    private void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<ItemStack> info) {
        if (world.isClient()) {
            TutorialModClient.recordBowUsage();
        }
    }
}
