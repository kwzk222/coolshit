package net.rev.tutorialmod.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;

public class MovementUtils {

    public static double computeDropDistance(PlayerEntity player, Box box) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return Double.MAX_VALUE;

        // Scan downward up to 1.2 blocks
        for (double y = 0; y <= 1.2; y += 0.05) {
            Box testBox = box.offset(0, -y, 0);

            boolean collides = mc.world
                    .getBlockCollisions(player, testBox)
                    .iterator()
                    .hasNext();

            if (collides) {
                return y;
            }
        }

        // No ground within range → large drop
        return Double.MAX_VALUE;
    }
}
