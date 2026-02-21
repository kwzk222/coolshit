package net.rev.tutorialmod.mixin;

import net.minecraft.client.render.Frustum;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Frustum.class)
public interface FrustumAccessor {
    @Invoker("init")
    void invokeInit(Matrix4f modelViewMatrix, Matrix4f projectionMatrix);
}
