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

        // Configurable hotkey activation (string-based like before)
        boolean keyPressed = false;
        try {
            keyPressed = InputUtil.isKeyPressed(mc.getWindow(),
                InputUtil.fromTranslationKey(TutorialMod.CONFIG.bridgeAssistHotkey).getCode());
        } catch (Exception ignored) {}

        boolean active = keyPressed && TutorialMod.CONFIG.masterEnabled && mc.currentScreen == null;

        if (!active) {
            // restore manual sneak only if we forced it previously
            if (lastAutoSneak) {
                mc.options.sneakKey.setPressed(isManualSneakPressed());
                lastAutoSneak = false;
            }
            sneakHoldCounter = 0;
            return;
        }

        var player = mc.player;

        // If not on ground, bail out but keep state safely restored
        if (!player.isOnGround()) {
            if (lastAutoSneak) {
                mc.options.sneakKey.setPressed(isManualSneakPressed());
                lastAutoSneak = false;
            }
            return;
        }

        Vec3d velocity = player.getVelocity();
        if (velocity.horizontalLengthSquared() < 1e-6) {
            // If nearly stationary, don't toggle sneak; keep existing state
            return;
        }

        Vec3d moveDir = new Vec3d(velocity.x, 0, velocity.z).normalize();
        if (moveDir.lengthSquared() < 1e-9) return;

        Box box = player.getBoundingBox();
        double half = 0.3;

        // center of player's footprint at ground level
        Vec3d footprintCenter = new Vec3d((box.minX + box.maxX) * 0.5, box.minY, (box.minZ + box.maxZ) * 0.5);

        // sample points projected along movement-facing edge (inset from corners)
        Vec3d edgeCenter = footprintCenter.add(moveDir.multiply(half + TutorialMod.CONFIG.bridgeAssistPredict));
        Vec3d lateral = new Vec3d(-moveDir.z, 0, moveDir.x).normalize();

        // Sample along the edge, but tucked in from the corners (0.15) to avoid side edges
        Vec3d[] samplePoints = new Vec3d[] {
            edgeCenter,
            edgeCenter.add(lateral.multiply(0.15)),
            edgeCenter.subtract(lateral.multiply(0.15))
        };

        // compute max drop among sample points (using box.minY as starting Y)
        double maxDrop = 0.0;
        for (Vec3d pt : samplePoints) {
            double drop = computeDropDistance(pt, box.minY);
            if (drop > maxDrop) maxDrop = drop;
        }

        boolean shouldSneak = lastAutoSneak;

        if (!lastAutoSneak && maxDrop > TutorialMod.CONFIG.bridgeAssistStartSneakHeight) {
            // ensure no ground at ANY sample point (small collision check)
            boolean anyGround = false;
            for (Vec3d pt : samplePoints) {
                Box groundCheck = new Box(pt.x, box.minY - 0.02, pt.z, pt.x + 0.001, box.minY + 0.001, pt.z + 0.001);
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

    private double computeDropDistance(Vec3d point, double startY) {
        if (mc.world == null) return Double.MAX_VALUE;
        for (double d = 0.0; d <= 1.5; d += 0.05) {
            // tiny test box at the sample x/z at height (startY - d)
            Box testBox = new Box(point.x, startY - d, point.z, point.x + 0.001, startY - d + 0.02, point.z + 0.001);
            boolean collides = mc.world.getBlockCollisions(mc.player, testBox).iterator().hasNext();
            if (collides) return d;
        }
        return Double.MAX_VALUE;
    }

    private boolean isManualSneakPressed() {
        try {
            return InputUtil.isKeyPressed(mc.getWindow(),
                InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
    }
}
