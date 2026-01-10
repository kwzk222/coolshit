package net.rev.tutorialmod.modules;

import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.rev.tutorialmod.ModConfig;
import net.rev.tutorialmod.TutorialMod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles chat-related features, including coordinate macros and custom chat macros.
 */
public class ChatMacros {

    private final Map<String, Boolean> wasMacroKeyPressed = new HashMap<>();

    /**
     * Initializes the chat and macro features.
     */
    public void onInitializeClient() {
        // Chat (public chat) modify event
        ClientSendMessageEvents.MODIFY_CHAT.register(message -> {
            ModConfig cfg = TutorialMod.CONFIG;
            if (!cfg.replaceInChat) return message;
            if (message == null) return message;

            String trigger = cfg.caseSensitive ? cfg.trigger : cfg.trigger.toLowerCase();
            String check = cfg.caseSensitive ? message : message.toLowerCase();

            // exact equality check for chat messages
            if (check.equals(trigger)) {
                return formatCoords(cfg);
            }

            return message;
        });

        // Command modify event: modifies the command string (without leading '/')
        ClientSendMessageEvents.MODIFY_COMMAND.register(command -> {
            ModConfig cfg = TutorialMod.CONFIG;
            if (!cfg.replaceInCommands) return command;
            if (command == null) return command;

            // build regex that only replaces whole-word occurrences of the trigger
            int flags = cfg.caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
            Pattern p = Pattern.compile("\\b" + Pattern.quote(cfg.trigger) + "\\b", flags);
            Matcher m = p.matcher(command);
            if (m.find()) {
                String coords = formatCoords(cfg);
                // safe replace all (quote replacement in case coords contain $ or \)
                return m.replaceAll(Matcher.quoteReplacement(coords));
            }
            return command;
        });
    }

    /**
     * Called every client tick to handle chat macros.
     * @param client The Minecraft client instance.
     */
    public void onClientTick(MinecraftClient client) {
        handleChatMacros(client);
    }

    private void handleChatMacros(MinecraftClient client) {
        if (client.player == null) return;

        List<ModConfig.Macro> macros = new ArrayList<>(Arrays.asList(TutorialMod.CONFIG.macro1, TutorialMod.CONFIG.macro2, TutorialMod.CONFIG.macro3, TutorialMod.CONFIG.macro4, TutorialMod.CONFIG.macro5));

        for (ModConfig.Macro macro : macros) {
            if (macro.hotkey == null || macro.hotkey.equals("key.keyboard.unknown") || macro.message == null || macro.message.isEmpty()) {
                continue;
            }

            boolean isPressed;
            try {
                isPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(macro.hotkey).getCode());
            } catch (IllegalArgumentException e) {
                continue; // Invalid key
            }

            boolean wasPressed = wasMacroKeyPressed.getOrDefault(macro.hotkey, false);

            if (isPressed && !wasPressed) {
                if (macro.message.startsWith("/")) {
                    client.player.networkHandler.sendChatCommand(macro.message.substring(1));
                } else {
                    client.player.networkHandler.sendChatMessage(macro.message);
                }
            }
            wasMacroKeyPressed.put(macro.hotkey, isPressed);
        }
    }

    private static String formatCoords(ModConfig cfg) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || player.getWorld() == null) return cfg.trigger; // fallback

        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        long bx = (long) Math.floor(x);
        long by = (long) Math.floor(y);
        long bz = (long) Math.floor(z);

        String sx = String.format("%.2f", x);
        String sy = String.format("%.2f", y);
        String sz = String.format("%.2f", z);

        // Dimension identifier
        String dim = "";
        try {
            RegistryKey<World> key = player.getWorld().getRegistryKey();
            Identifier id = key.getValue();
            if (id != null) {
                String dimensionName = id.getPath().replace("the_", "");
                dim = " " + dimensionName;
            }
        } catch (Exception ignored) {
        }

        // Facing (cardinal)
        String facing = "";
        try {
            Direction d = player.getHorizontalFacing();
            if (d != null) facing = " " + d.toString().toLowerCase();
        } catch (Exception ignored) {
        }

        // Compose replacements
        String out = cfg.format;
        out = out.replace("{x}", sx).replace("{y}", sy).replace("{z}", sz);
        out = out.replace("{bx}", Long.toString(bx)).replace("{by}", Long.toString(by)).replace("{bz}", Long.toString(bz));
        out = out.replace("{dim}", dim);
        out = out.replace("{facing}", facing);
        return out;
    }
}
