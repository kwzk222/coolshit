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
    private void tutorialmod$onBlockUpdate(
            BlockUpdateS2CPacket packet,
            CallbackInfo ci
    ) {
        if (TutorialModClient.pendingRailPos == null) return;

        if (packet.getPos().equals(TutorialModClient.pendingRailPos)
                && packet.getState().getBlock() instanceof net.minecraft.block.AbstractRailBlock) {

            MinecraftClient.getInstance().execute(() -> {
                TutorialModClient.placeTntMinecart(MinecraftClient.getInstance());
            });
            TutorialModClient.pendingRailPos = null;
        }
    }


    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (packet.getStatus() == 35 && packet.getEntity(client.player.getWorld()) == client.player) {
            if (!TutorialMod.CONFIG.autoTotemEnabled || !TutorialMod.CONFIG.autoTotemRefillOnPop) {
                return;
            }
            if (TutorialMod.CONFIG.autoTotemSurvivalOnly && client.player.isCreative()) {
                return;
            }
            if (TutorialModClient.getInstance() != null && TutorialModClient.getInstance().getAutoTotem() != null) {
                TutorialModClient.getInstance().getAutoTotem().handleTotemPop();
            }
        }
    }
}
