package net.rev.tutorialmod.modules.misc;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.rev.tutorialmod.ModConfig;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.MinecraftClientAccessor;

public class ClickSpamModule {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static double rightAccumulator = 0;

    public static void onTick() {
        if (mc.player == null || mc.world == null || mc.currentScreen != null) return;
        ModConfig config = TutorialMod.CONFIG;
        if (!config.masterEnabled || !config.clickSpamEnabled) return;

        boolean modifierHeld = isModifierHeld(config.clickSpamModifierKey);
        if (!modifierHeld) return;

        double delay = 20.0 / Math.max(1, config.clickSpamCps);

        // Right Click
        if (mc.options.useKey.isPressed()) {
            rightAccumulator++;
            while (rightAccumulator >= delay) {
                ((MinecraftClientAccessor) mc).invokeDoItemUse();
                ((MinecraftClientAccessor) mc).setItemUseCooldown(0);
                rightAccumulator -= delay;
            }
        } else {
            rightAccumulator = delay; // Ready for next press
        }
    }

    private static boolean isModifierHeld(String keyTranslation) {
        if (keyTranslation == null || keyTranslation.isEmpty()) return true;
        try {
            if (InputUtil.isKeyPressed(mc.getWindow(), org.lwjgl.glfw.GLFW.GLFW_KEY_F3)) {
                return false;
            }
            InputUtil.Key key = InputUtil.fromTranslationKey(keyTranslation);
            int code = key.getCode();
            if (code == -1) return true;
            return InputUtil.isKeyPressed(mc.getWindow(), code);
        } catch (Exception e) {
            return true;
        }
    }
}
