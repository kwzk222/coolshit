package net.rev.tutorialmod.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.rev.tutorialmod.modules.AutoCobweb;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerInventory.class)
public abstract class PlayerInventoryMixin {

    @Shadow public abstract ItemStack getStack(int slot);

    @Inject(method = "setSelectedSlot", at = @At("HEAD"))
    private void onSetSelectedSlot(int slot, CallbackInfo ci) {
        ItemStack stack = this.getStack(slot);
        if (stack.getItem() == Items.COBWEB) {
            AutoCobweb.onHotbarSwitch((PlayerInventory) (Object) this, slot);
        }
    }

    @Accessor("selectedSlot")
    public abstract void setSelectedSlot(int slot);

    @Accessor("selectedSlot")
    public abstract int getSelectedSlot();
}
