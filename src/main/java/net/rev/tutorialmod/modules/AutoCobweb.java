package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.humanmove.MathUtils;

import java.util.ArrayList;
import java.util.List;

public class AutoCobweb {
    private static final double MAX_RANGE = 5.0;

    public static void trigger() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }

        client.player.sendMessage(Text.literal("AutoCobweb triggered. Scanning for targets..."), false);

        PlayerEntity self = client.player;
        List<PlayerEntity> validTargets = new ArrayList<>();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == self) continue;
            if (TutorialMod.CONFIG.teamManager.isTeammate(player.getName().getString())) continue;
            if (self.distanceTo(player) > MAX_RANGE) continue;

            validTargets.add(player);
        }

        if (validTargets.isEmpty()) {
            client.player.sendMessage(Text.literal("No valid targets found."), false);
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
            client.player.sendMessage(Text.literal("Target found: " + bestTarget.getName().getString()), false);
            BlockPos targetBlockPos = bestTarget.getBlockPos().down();
            Vec3d targetVec = targetBlockPos.toCenterPos();

            client.player.sendMessage(Text.literal("Aiming at " + targetBlockPos), false);

            // Calculate yaw and pitch
            Vec3d d = targetVec.subtract(selfEyePos).normalize();
            float yaw = (float) (Math.atan2(d.z, d.x) * -180 / Math.PI) + 90f;
            float pitch = (float) (Math.asin(d.y) * -180 / Math.PI);

            // Set rotation
            self.setYaw(yaw);
            self.setPitch(pitch);

            // Place block
            client.player.sendMessage(Text.literal("Placing cobweb."), false);
            BlockHitResult hitResult = new BlockHitResult(targetVec, Direction.UP, targetBlockPos, false);
            if (client.interactionManager != null) {
                client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
            }
        } else {
            client.player.sendMessage(Text.literal("No valid targets found."), false);
        }
    }
}
