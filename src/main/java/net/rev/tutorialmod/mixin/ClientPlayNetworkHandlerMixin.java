package net.rev.tutorialmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
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

    @Inject(method = "onEntitySpawn", at = @At("HEAD"))
    private void onEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        if (TutorialModClient.awaitingMinecartConfirmationCooldown > 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                Entity entity = client.world.getEntityById(packet.getEntityId());
                if (entity instanceof net.minecraft.entity.vehicle.TntMinecartEntity) {
                    if (entity.getBlockPos().isWithinDistance(client.player.getBlockPos(), 5)) {
                        TutorialModClient.getInstance().startPostMinecartSequence(client);
                        TutorialModClient.awaitingMinecartConfirmationCooldown = -1;
                    }
                }
            }
        }
    }

    @Inject(method = "onEntityStatus", at = @At("HEAD"))
    private void onEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (packet.getStatus() == 35 && packet.getEntity(client.world) == client.player) {
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
