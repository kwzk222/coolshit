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
        if (velocity.horizontalLengthSquared() < 1e-4) {
            float forward = 0;
            if (mc.options.forwardKey.isPressed()) forward += 1;
            if (mc.options.backKey.isPressed()) forward -= 1;
            float sideways = 0;
            if (mc.options.leftKey.isPressed()) sideways += 1;
            if (mc.options.rightKey.isPressed()) sideways -= 1;

            if (forward == 0 && sideways == 0) {
                if (lastAutoSneak) {
                    mc.options.sneakKey.setPressed(true);
                } else {
                    mc.options.sneakKey.setPressed(false);
                }
                return;
            }

            float yaw = player.getYaw();
            if (forward < 0) yaw += 180;
            float sideYaw = 90;
            if (forward < 0) sideYaw = -90;
            if (sideways != 0) yaw -= sideways * sideYaw * (forward != 0 ? 0.5f : 1f);
            moveDir = Vec3d.fromPolar(0, yaw);
        } else {
            moveDir = new Vec3d(velocity.x, 0, velocity.z).normalize();
        }

        Box box = player.getBoundingBox();
        java.util.List<Vec3d> samplePoints = new java.util.ArrayList<>();
        double margin = 0.05;

        if (moveDir.x > 0.01) { // Moving East (+X), trailing is West (minX)
            for (double z = box.minZ; z <= box.maxZ; z += (box.maxZ - box.minZ) / 4.0) {
                samplePoints.add(new Vec3d(box.minX - margin, box.minY, z));
            }
        } else if (moveDir.x < -0.01) { // Moving West (-X), trailing is East (maxX)
            for (double z = box.minZ; z <= box.maxZ; z += (box.maxZ - box.minZ) / 4.0) {
                samplePoints.add(new Vec3d(box.maxX + margin, box.minY, z));
            }
        }

        if (moveDir.z > 0.01) { // Moving South (+Z), trailing is North (minZ)
            for (double x = box.minX; x <= box.maxX; x += (box.maxX - box.minX) / 4.0) {
                samplePoints.add(new Vec3d(x, box.minY, box.minZ - margin));
            }
        } else if (moveDir.z < -0.01) { // Moving North (-Z), trailing is South (maxZ)
            for (double x = box.minX; x <= box.maxX; x += (box.maxX - box.minX) / 4.0) {
                samplePoints.add(new Vec3d(x, box.minY, box.maxZ + margin));
            }
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
            sneakHoldCounter = 5; // Hysteresis: keep sneaking for at least 5 ticks
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
            InputUtil.Key key = InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey());
            if (key.getCategory() == InputUtil.Type.KEYSYM) {
                return InputUtil.isKeyPressed(mc.getWindow(), key.getCode());
            } else if (key.getCategory() == InputUtil.Type.MOUSE) {
                return org.lwjgl.glfw.GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), key.getCode()) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
            }
        } catch (Exception e) {}
        return false;
    }
}
