package net.rev.tutorialmod.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientPlayerInteractionManager.class)
public interface ClientPlayerInteractionManagerAccessor {
    @Accessor("selectedSlot")
    void setSelectedSlot(int selectedSlot);

    @Accessor("selectedSlot")
    int getSelectedSlot();

    @Invoker("syncSelectedSlot")
    void invokeSyncSelectedSlot();
}
