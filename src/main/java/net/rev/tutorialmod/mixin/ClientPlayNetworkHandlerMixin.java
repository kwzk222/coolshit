package net.rev.tutorialmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @Inject(method = "onBlockUpdate", at = @At("TAIL"))
    private void onBlockUpdate(BlockUpdateS2CPacket packet, CallbackInfo info) {
        TutorialModClient.confirmRailPlacement(packet.getPos(), packet.getState());
        TutorialModClient.confirmLavaPlacement(packet.getPos(), packet.getState());
        TutorialModClient.confirmFirePlacement(packet.getPos(), packet.getState());
    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        // Check if the packet is a totem pop for the current player
        if (client.player != null && packet.getStatus() == 35 && packet.getEntity(client.player.getWorld()) == client.player) {
            // Check if the feature and this specific sub-feature are enabled
            if (TutorialMod.CONFIG.autoTotemEnabled && TutorialMod.CONFIG.autoTotemRefillOnPop) {
                // Check for survival mode setting
                if (TutorialMod.CONFIG.autoTotemSurvivalOnly && client.player.isCreative()) {
                    return;
                }
                // Safely get the module and call the handler
                if (TutorialModClient.getInstance() != null && TutorialModClient.getInstance().getAutoTotem() != null) {
                    TutorialModClient.getInstance().getAutoTotem().handleTotemPop();
                }
            }
        }
    }
}
