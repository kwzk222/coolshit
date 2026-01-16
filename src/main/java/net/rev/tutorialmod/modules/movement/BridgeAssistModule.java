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
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
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
            return;
        }

        if (!player.isOnGround()) {
             // In air, we might want to release sneak if we were sneaking,
             // but usually Bridge Assist is for walking on edges.
             return;
        }

        Vec3d velocity = player.getVelocity();
        if (velocity.horizontalLengthSquared() < 1.0E-4) {
            player.setSneaking(false);
            return;
        }

        Vec3d direction = new Vec3d(velocity.x, 0, velocity.z).normalize();

        Box futureBox = player.getBoundingBox()
                .offset(direction.multiply(TutorialMod.CONFIG.parkourPredict));

        double drop = MovementUtils.computeDropDistance(player, futureBox);

        // Small drops (stairs, slabs) → no sneak
        if (drop <= TutorialMod.CONFIG.parkourMaxDropHeight) {
            player.setSneaking(false);
            return;
        }

        // Edge detected → sneak
        player.setSneaking(true);
    }
}
