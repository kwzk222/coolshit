package net.rev.tutorialmod.mixin;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerInventory.class)
public interface PlayerInventoryMixin {
    @Accessor("selectedSlot")
    void setSelectedSlot(int slot);

    @Accessor("selectedSlot")
    int getSelectedSlot();
}
