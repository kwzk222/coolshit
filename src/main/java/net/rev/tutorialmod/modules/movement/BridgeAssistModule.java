package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class BridgeAssistModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private boolean lastAutoSneak = false;

    public void init() {
        // Use START_CLIENT_TICK to affect current tick's movement
        ClientTickEvents.START_CLIENT_TICK.register(client -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null) return;

        // Hardcoded Left Alt activation
        boolean altPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), GLFW.GLFW_KEY_LEFT_ALT);
        boolean active = altPressed && TutorialMod.CONFIG.masterEnabled && mc.currentScreen == null;

        if (!active) {
            if (lastAutoSneak) {
                mc.options.sneakKey.setPressed(isManualSneakPressed());
                lastAutoSneak = false;
            }
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
            // If we are already auto-sneaking, keep it until we move or release Alt
            // This prevents falling if we stop at the very edge
            return;
        }

        Vec3d direction = new Vec3d(velocity.x, 0, velocity.z).normalize();

        Box futureBox = player.getBoundingBox()
                .offset(direction.multiply(TutorialMod.CONFIG.bridgeAssistPredict));

        // Find how far we would fall
        double drop = MovementUtils.computeDropDistance(player, futureBox);

        boolean shouldSneak = false;
        // If drop is large (not stairs/slabs) → potential edge
        if (drop > TutorialMod.CONFIG.bridgeAssistMaxDropHeight) {
            // If there is no ground support at all for the predicted position → sneak
            Box groundCheck = futureBox.offset(0.0, -0.01, 0.0);
            boolean hasGround = mc.world.getBlockCollisions(player, groundCheck).iterator().hasNext();
            shouldSneak = !hasGround;
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

    private boolean isManualSneakPressed() {
        try {
            return InputUtil.isKeyPressed(mc.getWindow().getHandle(),
                InputUtil.fromTranslationKey(mc.options.sneakKey.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
    }
}
