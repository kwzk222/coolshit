package net.rev.tutorialmod;

import net.fabricmc.api.ModInitializer;

import net.rev.tutorialmod.modules.AutoToolSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TutorialMod implements ModInitializer {
	public static final String MOD_ID = "tutorialmod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static ModConfig CONFIG;
	private static final AutoToolSwitch autoToolSwitch = new AutoToolSwitch();

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		CONFIG = ModConfig.load();
		LOGGER.info("Hello Fabric world!");
	}

	public static AutoToolSwitch getAutoToolSwitch() {
		return autoToolSwitch;
	}

	public static void sendUpdateMessage(String message) {
		net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
		if (client.player != null && !CONFIG.disableModChatUpdates) {
			client.player.sendMessage(net.minecraft.text.Text.literal("§7[§6TutorialMod§7] §f" + message), false);
		}
	}
}

