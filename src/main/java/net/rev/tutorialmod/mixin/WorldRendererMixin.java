package net.rev.tutorialmod.mixin;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.ObjectAllocator;
import net.rev.tutorialmod.TutorialModClient;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix, Matrix4f p_368142_, com.mojang.blaze3d.buffers.GpuBufferSlice p_366113_, Vector4f p_366057_, boolean p_368953_, CallbackInfo ci) {
        if (TutorialModClient.getInstance() != null && TutorialModClient.getInstance().getESPModule() != null) {
            TutorialModClient.getInstance().getESPModule().onRender(tickCounter, camera, projectionMatrix, modelViewMatrix);
        }
    }
}
