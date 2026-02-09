package net.rev.tutorialmod.modules.movement;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.rev.tutorialmod.TutorialMod;

import java.util.Random;

public class JumpResetModule {

    private static final Random RANDOM = new Random();
    private int jumpDelayTicks = -1;

    public void onTick(MinecraftClient client) {
        if (jumpDelayTicks > 0) {
            jumpDelayTicks--;
            if (jumpDelayTicks == 0) {
                jump(client);
                jumpDelayTicks = -1;
            }
        }
    }

    public void onHurt() {
        if (!TutorialMod.CONFIG.jumpResetEnabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (!isHotkeyHeld(client)) return;

        int delay = TutorialMod.CONFIG.jumpResetDelay;
        if (TutorialMod.CONFIG.jumpResetFailChance > 0 && RANDOM.nextInt(100) < TutorialMod.CONFIG.jumpResetFailChance) {
            delay += RANDOM.nextInt(TutorialMod.CONFIG.jumpResetMaxExtraDelay + 1);
        }

        if (delay == 0) {
            jump(client);
        } else {
            jumpDelayTicks = delay;
        }
    }

    private boolean isHotkeyHeld(MinecraftClient client) {
        try {
            if (InputUtil.isKeyPressed(client.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_F3)) {
                return false;
            }
            return net.rev.tutorialmod.TutorialModClient.isKeyDown(TutorialMod.CONFIG.jumpResetHotkey);
        } catch (Exception e) {
            return false;
        }
    }

    private void jump(MinecraftClient client) {
        client.execute(() -> {
            if (client.player != null) {
                // To simulate a press and release:
                client.options.jumpKey.setPressed(true);

                // Re-evaluating: client.player.jump() is what's usually used for auto-jump.
                if (client.player.isOnGround()) {
                    client.player.jump();
                }

                // Release it
                client.options.jumpKey.setPressed(false);
            }
        });
    }
}
