package net.rev.tutorialmod.mixin;

import net.minecraft.entity.LivingEntity;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.rev.tutorialmod.TutorialModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrossbowItem.class)
public class CrossbowItemMixin {
    @Inject(method = "onStoppedUsing", at = @At("TAIL"))
    private void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks, CallbackInfo info) {
        if (world.isClient) {
            TutorialModClient.confirmCrossbowShot();
        }
    }
}
