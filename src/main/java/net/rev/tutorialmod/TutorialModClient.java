package net.rev.tutorialmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.mixin.GameOptionsAccessor;
import net.rev.tutorialmod.modules.OverlayManager;

public class TutorialModClient implements ClientModInitializer {

    private final Map<String, Boolean> wasMacroKeyPressed = new HashMap<>();

    // --- Singleton Instance ---
    private static TutorialModClient instance;

    public static TutorialModClient getInstance() {
        return instance;
    }

    // --- Modules & Features ---
    private static OverlayManager overlayManager;

    public static OverlayManager getOverlayManager() {
        return overlayManager;
    }


    // --- Keybind States ---
    private boolean openSettingsWasPressed = false;
    private boolean overlayToggleWasPressed = false;
    private boolean sprintModeWasPressed = false;
    private boolean sneakModeWasPressed = false;

    // --- State: Misc ---
    private int pointingTickCounter = 0;
    private String lastLongCoordsInfo = null;


    @Override
    public void onInitializeClient() {
        instance = this;
        overlayManager = new OverlayManager();

        // Add shutdown hook to stop overlay process
        Runtime.getRuntime().addShutdownHook(new Thread(overlayManager::stop));

        // Register Event Listeners
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

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

    private void onClientTick(MinecraftClient client) {
        // Handle keybinds first, as they might toggle features on/off.
        handleKeybinds(client);
        handleChatMacros(client);

        // --- Centralized Overlay Logic ---
        boolean shouldOverlayBeRunning = TutorialMod.CONFIG.showCoordsOverlay;
        if (shouldOverlayBeRunning && !overlayManager.isRunning() && client.player != null) {
            overlayManager.start();
        } else if ((!shouldOverlayBeRunning || client.player == null) && overlayManager.isRunning()) {
            overlayManager.stop();
        }

        if (overlayManager.isRunning() && client.player != null) {
            if (TutorialMod.CONFIG.showCoordsOverlay) {
                overlayManager.update(formatCoordsForOverlay(client));
            } else {
                overlayManager.update(""); // Clear/Hide overlay
            }
        }
    }

    private void handleKeybinds(MinecraftClient client) {
        if (client.player == null) return;

        if (!TutorialMod.CONFIG.activeInInventory && client.currentScreen != null) {
            return;
        }

        // --- Open Settings Hotkey ---
        try {
            boolean isOpenSettingsPressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.openSettingsHotkey).getCode());
            if (isOpenSettingsPressed && !openSettingsWasPressed) {
                client.setScreen(new ModMenuIntegration().getModConfigScreenFactory().create(client.currentScreen));
            }
            openSettingsWasPressed = isOpenSettingsPressed;
        } catch (IllegalArgumentException e) {
            // Invalid key
        }

        // --- Toggle Overlay Hotkey ---
        try {
            boolean isToggleOverlayPressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.toggleOverlayHotkey).getCode());
            if (isToggleOverlayPressed && !overlayToggleWasPressed) {
                TutorialMod.CONFIG.showCoordsOverlay = !TutorialMod.CONFIG.showCoordsOverlay;
                TutorialMod.CONFIG.save();
                TutorialMod.sendUpdateMessage("Coords Overlay set to " + (TutorialMod.CONFIG.showCoordsOverlay ? "ON" : "OFF"));
                if (TutorialMod.CONFIG.showCoordsOverlay) {
                    getOverlayManager().start();
                } else {
                    getOverlayManager().stop();
                }
            }
            overlayToggleWasPressed = isToggleOverlayPressed;
        } catch (IllegalArgumentException e) {
            // Invalid key
        }

        // --- Toggle Sprint Mode Hotkey ---
        try {
            boolean isSprintModePressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.sprintModeHotkey).getCode());
            if (isSprintModePressed && !sprintModeWasPressed) {
                var sprintToggled = ((GameOptionsAccessor) client.options).getSprintToggled();
                boolean newValue = !sprintToggled.getValue();
                sprintToggled.setValue(newValue);
                // Reset state when switching to Hold mode
                if (!newValue) {
                    client.options.sprintKey.setPressed(isKeyCurrentlyPressed(client.options.sprintKey, client));
                }
                client.options.write();
                String mode = newValue ? "Toggle" : "Hold";
                TutorialMod.sendUpdateMessage("Sprint Mode set to " + mode);
            }
            sprintModeWasPressed = isSprintModePressed;
        } catch (IllegalArgumentException e) {
        }

        // --- Toggle Sneak Mode Hotkey ---
        try {
            boolean isSneakModePressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.sneakModeHotkey).getCode());
            if (isSneakModePressed && !sneakModeWasPressed) {
                var sneakToggled = ((GameOptionsAccessor) client.options).getSneakToggled();
                boolean newValue = !sneakToggled.getValue();
                sneakToggled.setValue(newValue);
                // Reset state when switching to Hold mode
                if (!newValue) {
                    client.options.sneakKey.setPressed(isKeyCurrentlyPressed(client.options.sneakKey, client));
                }
                client.options.write();
                String mode = newValue ? "Toggle" : "Hold";
                TutorialMod.sendUpdateMessage("Sneak Mode set to " + mode);
            }
            sneakModeWasPressed = isSneakModePressed;
        } catch (IllegalArgumentException e) {
        }
    }

    private boolean isKeyCurrentlyPressed(net.minecraft.client.option.KeyBinding keyBinding, MinecraftClient client) {
        try {
            return InputUtil.isKeyPressed(client.getWindow(),
                InputUtil.fromTranslationKey(keyBinding.getBoundKeyTranslationKey()).getCode());
        } catch (Exception e) {
            return false;
        }
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
                isPressed = InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(macro.hotkey).getCode());
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
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null) return cfg.trigger; // fallback

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
            RegistryKey<World> key = client.world.getRegistryKey();
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

    public String formatCoordsForOverlay(MinecraftClient client) {
        if (client.player == null || client.world == null) return "";

        String coords;
        if (TutorialMod.CONFIG.showAccurateCoordinates) {
            coords = String.format("%.3f %.3f %.3f", client.player.getX(), client.player.getY(), client.player.getZ());
        } else {
            coords = String.format("%d %d %d", (int) Math.floor(client.player.getX()), (int) Math.floor(client.player.getY()), (int) Math.floor(client.player.getZ()));
        }

        String facing = "";
        try {
            Direction d = client.player.getHorizontalFacing();
            if (d != null) {
                if (TutorialMod.CONFIG.showDetailedCardinals) {
                    facing = getDetailedFacing(client.player);
                } else {
                    // Capitalize first letter
                    facing = d.toString().substring(0, 1).toUpperCase() + d.toString().substring(1).toLowerCase();
                }
            }
        } catch (Exception ignored) {}

        StringBuilder result = new StringBuilder();
        result.append("Coords: ").append(coords);

        if (TutorialMod.CONFIG.showNetherCoords) {
            String converted = getConvertedCoords(client, client.player.getX(), client.player.getY(), client.player.getZ());
            if (converted != null) {
                result.append(" ").append(converted);
            }
        }

        result.append("\\nFacing: ").append(facing);

        if (TutorialMod.CONFIG.showChunkCount) {
            int completed = client.worldRenderer.getCompletedChunkCount();
            result.append(" C: ").append(completed);
        }

        if (TutorialMod.CONFIG.showEntityCount && client.world != null) {
            int entityCount = 0;
            for (Entity ignored : client.world.getEntities()) {
                entityCount++;
            }
            result.append(" E: ").append(entityCount);
        }

        if (TutorialMod.CONFIG.showLongCoords) {
            pointingTickCounter++;
            if (pointingTickCounter % 2 == 0 || lastLongCoordsInfo == null) {
                lastLongCoordsInfo = getLongCoordsInfo(client);
            }
            if (lastLongCoordsInfo != null) {
                result.append("\\n").append(lastLongCoordsInfo);
            }
        } else {
            lastLongCoordsInfo = null;
            pointingTickCounter = 0;
        }

        if (TutorialMod.CONFIG.showSprintModeOverlay || TutorialMod.CONFIG.showSneakModeOverlay) {
            result.append("\\n");
            if (TutorialMod.CONFIG.showSprintModeOverlay) {
                String mode = ((GameOptionsAccessor) client.options).getSprintToggled().getValue() ? "Toggle" : "Hold";
                result.append("Sprint: ").append(mode);
            }
            if (TutorialMod.CONFIG.showSneakModeOverlay) {
                String mode = ((GameOptionsAccessor) client.options).getSneakToggled().getValue() ? "Toggle" : "Hold";
                if (TutorialMod.CONFIG.showSprintModeOverlay) {
                    result.append(" | ");
                }
                result.append("Sneak: ").append(mode);
            }
        }

        return result.toString();
    }

    private String getLongCoordsInfo(MinecraftClient client) {
        if (client.player == null || client.world == null) return null;

        Vec3d origin = client.player.getCameraPosVec(1.0f);
        Vec3d direction = client.player.getRotationVec(1.0f);
        double maxDist = TutorialMod.CONFIG.longCoordsMaxDistance;

        // Check entities first
        Entity closestEntity = null;
        double minEntityDist = maxDist;

        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player) continue;
            net.minecraft.util.math.Box box = entity.getBoundingBox().expand(0.05);
            java.util.Optional<Vec3d> hit = box.raycast(origin, origin.add(direction.multiply(maxDist)));
            if (hit.isPresent()) {
                double dist = origin.distanceTo(hit.get());
                if (dist < minEntityDist) {
                    minEntityDist = dist;
                    closestEntity = entity;
                }
            }
        }

        // March the ray for blocks
        BlockPos hitPos = null;
        double step = 0.25;
        double blockDist = -1;

        for (double d = 0; d <= maxDist; d += step) {
            Vec3d point = origin.add(direction.multiply(d));
            BlockPos pos = BlockPos.ofFloored(point);

            // Manual check for unloaded chunks to avoid lag/ghost blocks
            if (!client.world.isChunkLoaded(pos.getX() >> 4, pos.getZ() >> 4)) {
                break;
            }

            BlockState state = client.world.getBlockState(pos);
            if (!state.isAir() && state.getFluidState().isEmpty()) {
                hitPos = pos;
                blockDist = d;
                break;
            }
        }

        if (closestEntity != null && (hitPos == null || minEntityDist < blockDist)) {
            String base = String.format("Pointing: %s", closestEntity.getName().getString());
            if (TutorialMod.CONFIG.showNetherCoords) {
                String converted = getConvertedCoords(client, closestEntity.getX(), closestEntity.getY(), closestEntity.getZ());
                if (converted != null) {
                    base += " " + converted;
                }
            }
            if (TutorialMod.CONFIG.showLongCoordsDistance) {
                base += String.format(" D: %d", (int)Math.round(minEntityDist));
            }
            return base;
        }

        if (hitPos != null) {
            String base = String.format("Pointing: %d %d %d", hitPos.getX(), hitPos.getY(), hitPos.getZ());
            if (TutorialMod.CONFIG.showNetherCoords) {
                String converted = getConvertedCoords(client, hitPos.getX(), hitPos.getY(), hitPos.getZ());
                if (converted != null) {
                    base += " " + converted;
                }
            }
            if (TutorialMod.CONFIG.showLongCoordsDistance) {
                base += String.format(" D: %d", (int)Math.round(blockDist));
            }
            return base;
        }

        return "Pointing: None";
    }

    private String getConvertedCoords(MinecraftClient client, double x, double y, double z) {
        if (client.world == null) return null;
        var dim = client.world.getRegistryKey();
        if (dim == World.OVERWORLD) {
            return String.format("N: %d %d %d", (int)Math.floor(x / 8.0), (int)Math.floor(y), (int)Math.floor(z / 8.0));
        } else if (dim == World.NETHER) {
            return String.format("O: %d %d %d", (int)Math.floor(x * 8.0), (int)Math.floor(y), (int)Math.floor(z * 8.0));
        }
        return null;
    }

    private String getDetailedFacing(PlayerEntity player) {
        float yaw = player.getYaw() % 360;
        if (yaw < 0) yaw += 360;

        String cardinal;
        String quadrant;

        if (yaw >= 337.5 || yaw < 22.5) {
            cardinal = "S";
            quadrant = "( _ + )";
        } else if (yaw >= 22.5 && yaw < 67.5) {
            cardinal = "SW";
            quadrant = "( - + )";
        } else if (yaw >= 67.5 && yaw < 112.5) {
            cardinal = "W";
            quadrant = "( - _ )";
        } else if (yaw >= 112.5 && yaw < 157.5) {
            cardinal = "NW";
            quadrant = "( - - )";
        } else if (yaw >= 157.5 && yaw < 202.5) {
            cardinal = "N";
            quadrant = "( _ - )";
        } else if (yaw >= 202.5 && yaw < 247.5) {
            cardinal = "NE";
            quadrant = "( + - )";
        } else if (yaw >= 247.5 && yaw < 292.5) {
            cardinal = "E";
            quadrant = "( + _ )";
        } else {
            cardinal = "SE";
            quadrant = "( + + )";
        }

        return cardinal + " " + quadrant;
    }

}
