package net.rev.tutorialmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {

    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), argsOnly = true)
    private String modifyChatMessage(String message) {
        if (message.contains("<")) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                Vec3d pos = client.player.getPos();
                String coords = String.format("(%d, %d, %d)", (int) pos.x, (int) pos.y, (int) pos.z);
                return message.replace("<", coords);
            }
        }
        return message;
    }
}
