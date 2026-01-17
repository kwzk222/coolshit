package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class ParkourModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.parkourEnabled) return;
            if (client.currentScreen != null) return;
            // Disable if Bridge Assist key is held
            try {
                if (InputUtil.isKeyPressed(client.getWindow().getHandle(),
                    InputUtil.fromTranslationKey(TutorialMod.CONFIG.bridgeAssistHotkey).getCode())) return;
            } catch (Exception ignored) {}
            tick();
        });
    }

    public void toggle() {
        TutorialMod.CONFIG.parkourEnabled = !TutorialMod.CONFIG.parkourEnabled;
        TutorialMod.CONFIG.save();
    }

    public boolean isEnabled() {
        return TutorialMod.CONFIG.parkourEnabled;
    }

    private void tick() {
        if (mc.player == null || mc.world == null) return;

        var player = mc.player;

        // Must be on ground - STRICT check to prevent flying
        if (!player.isOnGround()) return;

        // Do not interfere with sneaking
        if (player.isSneaking()) return;

        Vec3d velocity = player.getVelocity();

        // Ignore if not moving horizontally
        if (velocity.horizontalLengthSquared() < 1.0E-4) return;

        // Normalize horizontal movement direction
        Vec3d direction = new Vec3d(velocity.x, 0, velocity.z).normalize();

        // Predict forward movement (earlier jump = better distance)
        double predict = TutorialMod.CONFIG.parkourPredict;

        Box futureBox = player.getBoundingBox()
                .offset(direction.multiply(predict));

        // Find how far we would fall
        double dropDistance = MovementUtils.computeDropDistance(player, futureBox);

        // If drop is small (stairs, slabs), do NOT jump
        if (dropDistance <= TutorialMod.CONFIG.parkourMaxDropHeight) return;

        // If there is no ground support → jump
        Box groundCheck = futureBox.offset(0.0, -0.01, 0.0);

        boolean hasGround = mc.world
                .getBlockCollisions(player, groundCheck)
                .iterator()
                .hasNext();

        // If no collision → jump
        if (!hasGround) {
            player.jump();
        }
    }
}
