package net.rev.tutorialmod.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MinecraftClient.class)
public interface MinecraftClientAccessor {
    @Invoker("doItemUse")
    void invokeDoItemUse();

    @Accessor("itemUseCooldown")
    void setItemUseCooldown(int itemUseCooldown);

    @Invoker("doAttack")
    boolean invokeDoAttack();
}
