package net.rev.tutorialmod;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class Keybinds {
    public static KeyBinding EDGE_SNEAK_KEY;

    public static void register() {
        EDGE_SNEAK_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyBinding(
                "key.tutorialmod.edge_sneak",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_ALT,
                "category.tutorialmod.movement"
            )
        );
    }
}
