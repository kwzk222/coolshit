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

        // Module activates only when player is pressing Sneak
        if (!isManualSneakPressed()) {
            if (lastAutoSneak) {
                mc.options.sneakKey.setPressed(false);
                lastAutoSneak = false;
            }
            // Ensure manual sneak doesn't work if the module is ON but we aren't at an edge
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
        // Even if stationary, we might want to stay sneaked if we are at the edge
        // But the user said "direction they are walking"

        Vec3d moveDir;
        if (velocity.horizontalLengthSquared() < 1e-6) {
            // Use movement input if velocity is low
            float forward = 0;
            if (mc.options.forwardKey.isPressed()) forward += 1;
            if (mc.options.backKey.isPressed()) forward -= 1;
            float sideways = 0;
            if (mc.options.leftKey.isPressed()) sideways += 1;
            if (mc.options.rightKey.isPressed()) sideways -= 1;

            if (forward == 0 && sideways == 0) {
                if (!lastAutoSneak) {
                    mc.options.sneakKey.setPressed(false);
                }
                return;
            }
            Vec3d forwardVec = Vec3d.fromPolar(0, player.getYaw());
            Vec3d sideVec = Vec3d.fromPolar(0, player.getYaw() + 90);
            moveDir = forwardVec.multiply(forward).add(sideVec.multiply(sideways)).normalize();
        } else {
            moveDir = new Vec3d(velocity.x, 0, velocity.z).normalize();
        }

        Box box = player.getBoundingBox();

        // Find the "trailing" edge of the hitbox (opposite to moveDir)
        // We check a small margin around the trailing side.

        // Direction opposite to movement
        Vec3d oppositeDir = moveDir.multiply(-1);

        // Hitbox extent from center
        double hx = (box.maxX - box.minX) * 0.5;
        double hz = (box.maxZ - box.minZ) * 0.5;
        Vec3d center = box.getCenter().withAxis(net.minecraft.util.math.Direction.Axis.Y, box.minY);

        // The point at the very edge of the hitbox in the opposite direction
        // For diagonal movement, we check multiple points along that edge
        Vec3d trailingEdgePoint = center.add(
            Math.signum(oppositeDir.x) * hx,
            0,
            Math.signum(oppositeDir.z) * hz
        );

        // Actually, let's just check the whole footprint for ground.
        // We want to sneak if the player is about to fall off.
        // "side of the players hitbox oposite to the direction they are walking is at the very edge of a block"
        // This means most of the hitbox is already over air.

        // Check ground at the trailing edge.
        // We'll use a few sample points along the trailing side.
        Vec3d lateral = new Vec3d(-moveDir.z, 0, moveDir.x).normalize();

        // The center point of the trailing edge
        Vec3d trailingCenter = center.add(oppositeDir.normalize().multiply(Math.max(hx, hz)));
        // This is slightly wrong for diagonals.

        // Better: Find the point on the box perimeter in oppositeDir
        // For a box, the farthest point in direction d is (sign(d.x)*hx, sign(d.z)*hz)
        Vec3d backPoint = new Vec3d(
            center.x + (oppositeDir.x > 0.01 ? hx : (oppositeDir.x < -0.01 ? -hx : 0)),
            box.minY,
            center.z + (oppositeDir.z > 0.01 ? hz : (oppositeDir.z < -0.01 ? -hz : 0))
        );

        // Sample points along the back edge(s)
        // If vx and vz are both non-zero, it's a corner.

        boolean shouldSneak = false;
        double drop = computeDropDistance(backPoint, box.minY);
        if (drop > TutorialMod.CONFIG.bridgeAssistStartSneakHeight) {
            shouldSneak = true;
        }

        if (shouldSneak) {
            mc.options.sneakKey.setPressed(true);
            lastAutoSneak = true;
        } else {
            mc.options.sneakKey.setPressed(false);
            lastAutoSneak = false;
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
