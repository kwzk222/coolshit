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

        boolean active = TutorialMod.CONFIG.bridgeAssistEnabled && TutorialMod.CONFIG.masterEnabled && mc.currentScreen == null;

        if (!active) {
            if (lastAutoSneak) {
                mc.options.sneakKey.setPressed(isManualSneakPressed());
                lastAutoSneak = false;
            }
            return;
        }

        // Module activates only when player is pressing Sneak key
        if (!isManualSneakPressed()) {
            if (lastAutoSneak) {
                mc.options.sneakKey.setPressed(false);
                lastAutoSneak = false;
            }
            mc.options.sneakKey.setPressed(false);
            return;
        }

        var player = mc.player;

        // If not on ground, don't auto-sneak
        if (!player.isOnGround()) {
            mc.options.sneakKey.setPressed(false);
            lastAutoSneak = false;
            return;
        }

        Vec3d velocity = player.getVelocity();
        Vec3d moveDir;
        if (velocity.horizontalLengthSquared() < 1e-6) {
            float forward = 0;
            if (mc.options.forwardKey.isPressed()) forward += 1;
            if (mc.options.backKey.isPressed()) forward -= 1;
            float sideways = 0;
            if (mc.options.leftKey.isPressed()) sideways += 1;
            if (mc.options.rightKey.isPressed()) sideways -= 1;

            if (forward == 0 && sideways == 0) {
                // Stationary - maintain current sneak if we were already sneaking
                if (lastAutoSneak) {
                    mc.options.sneakKey.setPressed(true);
                } else {
                    mc.options.sneakKey.setPressed(false);
                }
                return;
            }
            // sideways is positive for Left, negative for Right in Input but here we used keys directly.
            // In MC polar, 0 is South (+Z), -90 is East (+X).
            Vec3d fwd = Vec3d.fromPolar(0, player.getYaw());
            Vec3d side = Vec3d.fromPolar(0, player.getYaw() - 90); // Left
            moveDir = fwd.multiply(forward).add(side.multiply(sideways)).normalize();
        } else {
            moveDir = new Vec3d(velocity.x, 0, velocity.z).normalize();
        }

        Box box = player.getBoundingBox();
        java.util.List<Vec3d> samplePoints = new java.util.ArrayList<>();

        // Define trailing sides based on movement direction
        if (moveDir.x > 0.1) { // Moving East (+X), trailing is West (minX)
            samplePoints.add(new Vec3d(box.minX, box.minY, box.minZ));
            samplePoints.add(new Vec3d(box.minX, box.minY, box.maxZ));
            samplePoints.add(new Vec3d(box.minX, box.minY, (box.minZ + box.maxZ) * 0.5));
        } else if (moveDir.x < -0.1) { // Moving West (-X), trailing is East (maxX)
            samplePoints.add(new Vec3d(box.maxX, box.minY, box.minZ));
            samplePoints.add(new Vec3d(box.maxX, box.minY, box.maxZ));
            samplePoints.add(new Vec3d(box.maxX, box.minY, (box.minZ + box.maxZ) * 0.5));
        }

        if (moveDir.z > 0.1) { // Moving South (+Z), trailing is North (minZ)
            samplePoints.add(new Vec3d(box.minX, box.minY, box.minZ));
            samplePoints.add(new Vec3d(box.maxX, box.minY, box.minZ));
            samplePoints.add(new Vec3d((box.minX + box.maxX) * 0.5, box.minY, box.minZ));
        } else if (moveDir.z < -0.1) { // Moving North (-Z), trailing is South (maxZ)
            samplePoints.add(new Vec3d(box.minX, box.minY, box.maxZ));
            samplePoints.add(new Vec3d(box.maxX, box.minY, box.maxZ));
            samplePoints.add(new Vec3d((box.minX + box.maxX) * 0.5, box.minY, box.maxZ));
        }

        boolean shouldSneak = false;
        for (Vec3d pt : samplePoints) {
            if (computeDropDistance(pt, box.minY) > TutorialMod.CONFIG.bridgeAssistStartSneakHeight) {
                shouldSneak = true;
                break;
            }
        }

        if (shouldSneak) {
            mc.options.sneakKey.setPressed(true);
            lastAutoSneak = true;
            sneakHoldCounter = 3; // Hysteresis: keep sneaking for at least 3 ticks
        } else {
            if (sneakHoldCounter > 0) {
                sneakHoldCounter--;
                mc.options.sneakKey.setPressed(true);
                lastAutoSneak = true;
            } else {
                mc.options.sneakKey.setPressed(false);
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
