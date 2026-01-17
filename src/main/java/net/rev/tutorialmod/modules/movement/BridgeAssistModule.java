package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class BridgeAssistModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean lastAutoSneak = false;
    private int sneakHoldCounter = 0;

    public void init() {
        // Use START_CLIENT_TICK to affect current tick's movement
        ClientTickEvents.START_CLIENT_TICK.register(client -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null) return;

        // Configurable hotkey activation
        boolean keyPressed = false;
        try {
            keyPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                InputUtil.fromTranslationKey(TutorialMod.CONFIG.bridgeAssistHotkey).getCode());
        } catch (Exception ignored) {}

        boolean active = keyPressed && TutorialMod.CONFIG.masterEnabled && mc.currentScreen == null;

        if (!active) {
            if (lastAutoSneak) {
                mc.options.sneakKey.setPressed(isManualSneakPressed());
                lastAutoSneak = false;
            }
            sneakHoldCounter = 0;
            return;
        }

        var player = mc.player;

        // Must be on ground
        if (!player.isOnGround()) {
            if (lastAutoSneak) {
                mc.options.sneakKey.setPressed(isManualSneakPressed());
                lastAutoSneak = false;
            }
            return;
        }

        Vec3d velocity = player.getVelocity();
        // Ignore if not moving horizontally
        if (velocity.horizontalLengthSquared() < 1.0E-6) {
            // Keep current sneak state while stationary to avoid falling
            return;
        }

        Vec3d moveDir = new Vec3d(velocity.x, 0, velocity.z).normalize();
        Box box = player.getBoundingBox();
        double half = 0.3; // player half-width

        // Project the sampling points onto the leading edge of the movement
        Vec3d edgeCenter = new Vec3d(
            (box.minX + box.maxX) / 2,
            box.minY,
            (box.minZ + box.maxZ) / 2
        ).add(moveDir.multiply(half + TutorialMod.CONFIG.bridgeAssistPredict));

        Vec3d lateral = new Vec3d(-moveDir.z, 0, moveDir.x).normalize();

        // Sample along the edge, but tucked in from the corners (0.15 instead of 0.3) to avoid side edges
        Vec3d[] samplePoints = new Vec3d[] {
            edgeCenter,
            edgeCenter.add(lateral.multiply(0.15)),
            edgeCenter.subtract(lateral.multiply(0.15))
        };

        // Check the maximum drop among the sample points
        double maxDrop = 0;
        for (Vec3d pt : samplePoints) {
            double drop = computeDropDistance(pt);
            if (drop > maxDrop) maxDrop = drop;
        }

        boolean shouldSneak = lastAutoSneak;

        if (!lastAutoSneak && maxDrop > TutorialMod.CONFIG.bridgeAssistStartSneakHeight) {
            // Check if there's REALLY no ground support at any of the sample points
            boolean anyGround = false;
            for (Vec3d pt : samplePoints) {
                Box groundCheck = new Box(pt.x, box.minY - 0.01, pt.z, pt.x + 0.001, box.minY, pt.z + 0.001);
                if (mc.world.getBlockCollisions(player, groundCheck).iterator().hasNext()) {
                    anyGround = true;
                    break;
                }
            }
            if (!anyGround) {
                shouldSneak = true;
                sneakHoldCounter = TutorialMod.CONFIG.bridgeAssistMinHoldTicks;
            }
        } else if (lastAutoSneak) {
            if (maxDrop < TutorialMod.CONFIG.bridgeAssistStopSneakHeight && sneakHoldCounter <= 0) {
                shouldSneak = false;
            } else {
                sneakHoldCounter--;
            }
        }

        if (shouldSneak) {
            mc.options.sneakKey.setPressed(true);
            lastAutoSneak = true;
        } else {
            if (lastAutoSneak) {
                mc.options.sneakKey.setPressed(isManualSneakPressed());
                lastAutoSneak = false;
            }
        }
    }

    private double computeDropDistance(Vec3d point) {
        if (mc.world == null || mc.player == null) return Double.MAX_VALUE;
        double y = mc.player.getY();
        for (double d = 0; d <= 1.2; d += 0.05) {
            Box testBox = new Box(point.x, y - d, point.z, point.x + 0.001, y - d + 0.01, point.z + 0.001);
            boolean collides = mc.world.getBlockCollisions(mc.player, testBox).iterator().hasNext();
            if (collides) return d;
        }
        return Double.MAX_VALUE;
    }

    private boolean isManualSneakPressed() {
        try {
            return InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
    }
}
