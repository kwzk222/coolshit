package net.rev.tutorialmod.mixin;

import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityS2CPacket.class)
public interface EntityS2CPacketAccessor {
    @Accessor("id")
    int getId();

    @Accessor("deltaX")
    short getDeltaX();

    @Accessor("deltaY")
    short getDeltaY();

    @Accessor("deltaZ")
    short getDeltaZ();
}
