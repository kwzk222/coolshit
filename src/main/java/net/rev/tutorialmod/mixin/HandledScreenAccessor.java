package net.rev.tutorialmod.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("focusedSlot")
    Slot getFocusedSlot();

    @Accessor("focusedSlot")
    void setFocusedSlot(Slot slot);

    @Invoker("onMouseClick")
    void invokeOnMouseClick(Slot slot, int slotId, int button, SlotActionType actionType);
}
