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
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityS2CPacket;
import net.rev.tutorialmod.mixin.EntityS2CPacketAccessor;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
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

    @Inject(method = "onEntitySpawn", at = @At("TAIL"))
    private void onEntitySpawn(EntitySpawnS2CPacket packet, CallbackInfo ci) {
        if (TutorialModClient.getInstance() != null && TutorialModClient.getInstance().getESPModule() != null) {
            TutorialModClient.getInstance().getESPModule().updateVanishedPlayer(packet.getEntityId(), packet.getX(), packet.getY(), packet.getZ());
        }
        if (TutorialModClient.awaitingMinecartConfirmationCooldown > 0) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                if (packet.getEntityType() == net.minecraft.entity.EntityType.TNT_MINECART) {
                    double distSq = client.player.squaredDistanceTo(packet.getX(), packet.getY(), packet.getZ());
                    if (distSq <= 25.0) {
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

        if (packet.getStatus() == 30) {
            Entity entity = packet.getEntity(client.world);
            if (entity != null && TutorialModClient.getInstance() != null) {
                TutorialModClient.getInstance().onShieldBreak(entity.getId());
            }
        }
    }

    @Inject(method = "onEntityPosition", at = @At("HEAD"))
    private void onEntityPosition(EntityPositionS2CPacket packet, CallbackInfo ci) {
        if (TutorialModClient.getInstance() != null && TutorialModClient.getInstance().getESPModule() != null) {
            TutorialModClient.getInstance().getESPModule().updateVanishedPlayer(packet.entityId(), packet.change().position().x, packet.change().position().y, packet.change().position().z);
        }
    }

    @Inject(method = "onEntity", at = @At("HEAD"))
    private void onEntity(EntityS2CPacket packet, CallbackInfo ci) {
        if (TutorialModClient.getInstance() != null && TutorialModClient.getInstance().getESPModule() != null) {
            EntityS2CPacketAccessor accessor = (EntityS2CPacketAccessor) packet;
            TutorialModClient.getInstance().getESPModule().updateVanishedPlayerRelative(accessor.getId(), accessor.getDeltaX(), accessor.getDeltaY(), accessor.getDeltaZ());
        }
    }

    @Inject(method = "onEntityAttributes", at = @At("HEAD"))
    private void onEntityAttributes(EntityAttributesS2CPacket packet, CallbackInfo ci) {
        if (TutorialModClient.getInstance() != null && TutorialModClient.getInstance().getESPModule() != null) {
            // Just ensure it's tracked if it doesn't exist. We don't have pos here, but next Move packet will catch it.
        }
    }

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void onGameJoin(GameJoinS2CPacket packet, CallbackInfo ci) {
        if (TutorialModClient.getInstance() != null) {
            TutorialModClient.getInstance().clearShieldCooldowns();
            if (TutorialModClient.getInstance().getESPModule() != null) {
                TutorialModClient.getInstance().getESPModule().syncWindowBounds();
            }
        }
    }
}
