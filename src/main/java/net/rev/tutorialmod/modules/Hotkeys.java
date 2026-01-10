package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;

/**
 * Handles all hotkey-related logic for the mod.
 */
public class Hotkeys {

    private boolean masterToggleWasPressed = false;
    private boolean teammateWasPressed = false;
    private boolean triggerBotToggleWasPressed = false;
    private boolean toggleSneakWasPressed = false;
    private boolean toggleSprintWasPressed = false;
    private boolean overlayToggleWasPressed = false;

    /**
     * Called every client tick to handle hotkeys.
     * @param client The Minecraft client instance.
     */
    public void onClientTick(MinecraftClient client) {
        handleKeybinds(client);
        handleToggleKeys(client);
    }

    private void handleKeybinds(MinecraftClient client) {
        if (client.player == null) return;

        if (!TutorialMod.CONFIG.activeInInventory && client.currentScreen != null) {
            return;
        }

        try {
            boolean isMasterTogglePressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.masterToggleHotkey).getCode());
            if (isMasterTogglePressed && !masterToggleWasPressed) {
                TutorialMod.CONFIG.masterEnabled = !TutorialMod.CONFIG.masterEnabled;
                TutorialMod.CONFIG.save();
                client.player.sendMessage(Text.of("TutorialMod Master Switch: " + (TutorialMod.CONFIG.masterEnabled ? "ON" : "OFF")), false);
            }
            masterToggleWasPressed = isMasterTogglePressed;
        } catch (IllegalArgumentException e) {
            // This can happen if the key is not set or invalid.
        }


        try {
            boolean isTeammateKeyPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.teammateHotkey).getCode());
            if (isTeammateKeyPressed && !teammateWasPressed) {
                handleTeammateKeybind(client);
            }
            teammateWasPressed = isTeammateKeyPressed;
        } catch (IllegalArgumentException e) {
        }


        try {
            boolean isTriggerBotTogglePressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.triggerBotToggleHotkey).getCode());
            if (isTriggerBotTogglePressed && !triggerBotToggleWasPressed) {
                TutorialMod.CONFIG.triggerBotToggledOn = !TutorialMod.CONFIG.triggerBotToggledOn;
                client.player.sendMessage(Text.of("TriggerBot: " + (TutorialMod.CONFIG.triggerBotToggledOn ? "ON" : "OFF")), false);
            }
            triggerBotToggleWasPressed = isTriggerBotTogglePressed;
        } catch (IllegalArgumentException e) {
        }

        // --- Toggle Sneak Hotkey ---
        try {
            boolean isToggleSneakPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.toggleSneakHotkey).getCode());
            if (isToggleSneakPressed && !toggleSneakWasPressed) {
                TutorialMod.CONFIG.isToggleSneakOn = !TutorialMod.CONFIG.isToggleSneakOn;
                client.player.sendMessage(Text.of("Toggle Sneak: " + (TutorialMod.CONFIG.isToggleSneakOn ? "ON" : "OFF")), false);

                if (TutorialMod.CONFIG.isToggleSneakOn) {
                    if (TutorialMod.CONFIG.isToggleSprintOn) {
                        TutorialMod.CONFIG.isToggleSprintOn = false;
                        client.player.sendMessage(Text.of("Toggle Sprint: OFF"), false);
                        client.options.sprintKey.setPressed(false);
                    }
                } else {
                    client.options.sneakKey.setPressed(false);
                }
            }
            toggleSneakWasPressed = isToggleSneakPressed;
        } catch (IllegalArgumentException e) {
        }

        // --- Toggle Sprint Hotkey ---
        try {
            boolean isToggleSprintPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.toggleSprintHotkey).getCode());
            if (isToggleSprintPressed && !toggleSprintWasPressed) {
                TutorialMod.CONFIG.isToggleSprintOn = !TutorialMod.CONFIG.isToggleSprintOn;
                client.player.sendMessage(Text.of("Toggle Sprint: " + (TutorialMod.CONFIG.isToggleSprintOn ? "ON" : "OFF")), false);

                if (TutorialMod.CONFIG.isToggleSprintOn) {
                    if (TutorialMod.CONFIG.isToggleSneakOn) {
                        TutorialMod.CONFIG.isToggleSneakOn = false;
                        client.player.sendMessage(Text.of("Toggle Sneak: OFF"), false);
                        client.options.sneakKey.setPressed(false);
                    }
                } else {
                    client.options.sprintKey.setPressed(false);
                }
            }
            toggleSprintWasPressed = isToggleSprintPressed;
        } catch (IllegalArgumentException e) {
        }

        // --- Toggle Overlay Hotkey ---
        try {
            boolean isToggleOverlayPressed = InputUtil.isKeyPressed(client.getWindow().getHandle(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.toggleOverlayHotkey).getCode());
            if (isToggleOverlayPressed && !overlayToggleWasPressed) {
                TutorialMod.CONFIG.showCoordsOverlay = !TutorialMod.CONFIG.showCoordsOverlay;
                TutorialMod.CONFIG.save();
                client.player.sendMessage(Text.of("Coords Overlay: " + (TutorialMod.CONFIG.showCoordsOverlay ? "ON" : "OFF")), false);
                if (TutorialMod.CONFIG.showCoordsOverlay) {
                    // Assuming you have a way to get the OverlayManager instance
                    // For now, let's say it's accessible via a static getter in TutorialModClient
                    net.rev.tutorialmod.TutorialModClient.getOverlayManager().start();
                } else {
                    net.rev.tutorialmod.TutorialModClient.getOverlayManager().stop();
                }
            }
            overlayToggleWasPressed = isToggleOverlayPressed;
        } catch (IllegalArgumentException e) {
            // Invalid key
        }
    }

    private void handleTeammateKeybind(MinecraftClient client) {
        PlayerEntity target = getPlayerLookingAt(client, 9.0);
        if (target != null) {
            String name = target.getName().getString();
            if (TutorialMod.CONFIG.teamManager.isTeammate(name)) {
                TutorialMod.CONFIG.teamManager.removeTeammate(name);
                client.player.sendMessage(Text.of("Removed " + name + " from your team."), false);
            } else {
                if (TutorialMod.CONFIG.teamManager.addTeammate(name)) {
                    client.player.sendMessage(Text.of("Added " + name + " to your team."), false);
                }
            }
        }
    }

    private void handleToggleKeys(MinecraftClient client) {
        if (client.player == null) return;
        // If master switch is off, ensure all toggles are disabled.
        if (!TutorialMod.CONFIG.masterEnabled) {
            if (TutorialMod.CONFIG.isToggleSneakOn) {
                TutorialMod.CONFIG.isToggleSneakOn = false;
                client.options.sneakKey.setPressed(false);
            }
            if (TutorialMod.CONFIG.isToggleSprintOn) {
                TutorialMod.CONFIG.isToggleSprintOn = false;
                client.options.sprintKey.setPressed(false);
            }
            return;
        }

        // --- Handle Toggle Sneak ---
        if (TutorialMod.CONFIG.isToggleSneakOn) {
            client.options.sneakKey.setPressed(true);
        }

        // --- Handle Toggle Sprint ---
        if (TutorialMod.CONFIG.isToggleSprintOn) {
            client.options.sprintKey.setPressed(true);
        }
    }

    private PlayerEntity getPlayerLookingAt(MinecraftClient client, double maxDistance) {
        PlayerEntity foundPlayer = null;
        double maxDot = -1.0; // cos(180)

        if (client.world == null || client.player == null) {
            return null;
        }

        Vec3d cameraPos = client.player.getEyePos();
        Vec3d lookVec = client.player.getRotationVector();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player) continue;

            double distance = client.player.distanceTo(player);
            if (distance > maxDistance) continue;

            Vec3d directionToPlayer = player.getEyePos().subtract(cameraPos).normalize();
            double dot = lookVec.dotProduct(directionToPlayer);

            // A larger dot product means a smaller angle.
            // We'll use a threshold of 0.99, which is about 8 degrees.
            if (dot > 0.99 && dot > maxDot) {
                maxDot = dot;
                foundPlayer = player;
            }
        }
        return foundPlayer;
    }
}
