package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.Human;
import net.rev.tutorialmod.TutorialMod;

import java.util.ArrayList;
import java.util.List;

public class AutoCobweb {
    private static final double MAX_RANGE = 5.0;

    public static void onHotbarSwitch(PlayerInventory inventory, int slot) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        PlayerEntity self = client.player;
        List<PlayerEntity> validTargets = new ArrayList<>();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == self) continue;
            if (TutorialMod.CONFIG.teamManager.isTeammate(player.getName().getString())) continue;
            if (self.distanceTo(player) > MAX_RANGE) continue;

            validTargets.add(player);
        }

        if (validTargets.isEmpty()) {
            return;
        }

        PlayerEntity bestTarget = null;
        double minAngle = Double.MAX_VALUE;

        Vec3d selfEyePos = self.getEyePos();
        Vec3d lookVec = self.getRotationVector();

        for (PlayerEntity target : validTargets) {
            Vec3d directionToTarget = target.getPos().subtract(selfEyePos).normalize();
            double angle = Math.acos(lookVec.dotProduct(directionToTarget));
            if (angle < minAngle) {
                minAngle = angle;
                bestTarget = target;
            }
        }

        if (bestTarget != null) {
            BlockPos targetBlockPos = bestTarget.getBlockPos().down();
            Vec3d targetVec = targetBlockPos.toCenterPos();

            Runnable placeCobweb = () -> {
                BlockHitResult hitResult = new BlockHitResult(targetVec, Direction.UP, targetBlockPos, false);
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            };

            Human.move().lookAt(targetVec, placeCobweb);
        }
    }
}
