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
        } else if (jumpDelayTicks == 0) {
            jump(client);
            jumpDelayTicks = -1;
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
            return InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.jumpResetHotkey).getCode());
        } catch (Exception e) {
            return false;
        }
    }

    private void jump(MinecraftClient client) {
        if (client.player != null) {
            client.options.jumpKey.setPressed(true);
            // We'll release it next tick or just let Minecraft handle it if it was already pressed.
            // Actually, for jump reset, just triggering the jump is enough.
            client.execute(() -> {
                // This runs on the client thread.
                // To simulate a press and release:
                // Actually, client.player.jump() might be better if we want it immediate,
                // but setting jumpKey.setPressed(true) is more "authentic".
                // But we need to make sure it gets released.
                // MinecraftClient.doAttack/doItemUse don't help here.
            });

            // Re-evaluating: client.player.jump() is what's usually used for auto-jump.
            if (client.player.isOnGround()) {
                client.player.jump();
            }
            client.options.jumpKey.setPressed(false);
        }
    }
}
