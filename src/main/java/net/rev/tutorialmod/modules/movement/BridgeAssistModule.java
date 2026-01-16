package net.rev.tutorialmod.modules.movement;

import net.rev.tutorialmod.TutorialMod;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class BridgeAssistModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void init() {
        // Use START_CLIENT_TICK as recommended
        ClientTickEvents.START_CLIENT_TICK.register(client -> tick());
    }

    private void tick() {
        if (mc.player == null || mc.world == null) return;
        if (!TutorialMod.CONFIG.masterEnabled) return;

        var player = mc.player;

        // Only active while key is held
        boolean isPressed;
        try {
            isPressed = InputUtil.isKeyPressed(mc.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.bridgeAssistHotkey).getCode());
        } catch (Exception e) {
            isPressed = false;
        }

        if (!isPressed) {
            if (player.isSneaking() && !mc.options.sneakKey.isPressed()) {
                player.setSneaking(false);
            }
            return;
        }

        // Improved isOnGround check
        if (!player.isOnGround() && player.fallDistance > 0.05f) {
            return;
        }

        Vec3d velocity = player.getVelocity();
        // Lower velocity threshold
        if (velocity.horizontalLengthSquared() < 1.0E-6) {
            if (player.isSneaking() && !mc.options.sneakKey.isPressed()) {
                player.setSneaking(false);
            }
            return;
        }

        Vec3d direction = new Vec3d(velocity.x, 0, velocity.z).normalize();

        Box futureBox = player.getBoundingBox()
                .offset(direction.multiply(TutorialMod.CONFIG.parkourPredict));

        double drop = MovementUtils.computeDropDistance(player, futureBox);

        // Small drops (stairs, slabs) → no sneak
        if (drop <= TutorialMod.CONFIG.parkourMaxDropHeight) {
            if (player.isSneaking() && !mc.options.sneakKey.isPressed()) {
                player.setSneaking(false);
            }
            return;
        }

        // Edge detected → sneak
        player.setSneaking(true);
    }
}
