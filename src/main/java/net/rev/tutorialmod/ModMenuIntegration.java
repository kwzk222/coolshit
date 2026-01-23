package net.rev.tutorialmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.rev.tutorialmod.mixin.ScreenAccessor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("TutorialMod Settings"))
                    .setDefaultBackgroundTexture(Identifier.ofVanilla("textures/gui/options_background.png"));

            builder.setSavingRunnable(TutorialMod.CONFIG::save);
            builder.setDoesConfirmSave(false); // We want direct save

            builder.setAfterInitConsumer(screen -> {
                // Add a "Save" button to the bottom right area
                int buttonWidth = 60;
                int buttonHeight = 20;
                int x = screen.width - buttonWidth - 10;
                int y = screen.height - 26;

                ButtonWidget saveButton = ButtonWidget.builder(Text.literal("Save"), button -> {
                    TutorialMod.CONFIG.save();
                    TutorialMod.sendUpdateMessage("Settings saved successfully.");
                }).dimensions(x, y, buttonWidth, buttonHeight).build();

                // Use accessor to call protected addDrawableChild
                ((ScreenAccessor) screen).invokeAddDrawableChild(saveButton);
            });

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();


            // Attribute Swapping Category
            ConfigCategory autoStun = builder.getOrCreateCategory(Text.literal("Attribute Swapping"));

            // Shared/General Settings
            autoStun.addEntry(entryBuilder.startBooleanToggle(Text.literal("Facing Check Enabled"), TutorialMod.CONFIG.autoStunFacingCheck)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, AutoStun will only trigger if the target is facing you (correct for shield blocks)."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoStunFacingCheck = newValue)
                    .build());
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("Axe to Original Delay"), TutorialMod.CONFIG.axeToOriginalDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay (ticks) before swapping back from the axe to the original item (in sequence)."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeToOriginalDelay = newValue)
                    .build());
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("Mace to Original Delay"), TutorialMod.CONFIG.maceToOriginalDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay (ticks) before swapping back from the mace to the original item (in sequence)."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceToOriginalDelay = newValue)
                    .build());

            // Sword AutoStun SubCategory
            SubCategoryBuilder axeStunSub = entryBuilder.startSubCategory(Text.literal("Axe AutoStun (Sword)"));
            axeStunSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.axeSwapEnabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapEnabled = newValue).build());
            axeStunSub.add(entryBuilder.startLongSlider(Text.literal("Range"), (long)(TutorialMod.CONFIG.axeSwapRange * 10), 0, 60)
                    .setDefaultValue(31)
                    .setTooltip(Text.literal("Maximum range (blocks * 10) to trigger."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapRange = newValue / 10.0).build());
            axeStunSub.add(entryBuilder.startIntSlider(Text.literal("Back Delay"), TutorialMod.CONFIG.axeSwapDelay, 0, 20)
                    .setDefaultValue(1)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapDelay = newValue).build());
            axeStunSub.add(entryBuilder.startIntSlider(Text.literal("Fail Chance"), TutorialMod.CONFIG.axeSwapFailChance, 0, 100)
                    .setDefaultValue(0)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapFailChance = newValue).build());
            axeStunSub.add(entryBuilder.startIntSlider(Text.literal("Prediction Chance"), TutorialMod.CONFIG.axeSwapFakePredictionChance, 0, 100)
                    .setDefaultValue(0)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapFakePredictionChance = newValue).build());
            autoStun.addEntry(axeStunSub.build());

            // Mace AutoStun SubCategory
            SubCategoryBuilder maceStunSub = entryBuilder.startSubCategory(Text.literal("Axe AutoStun (Mace)"));
            maceStunSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.maceAutoStunEnabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceAutoStunEnabled = newValue).build());
            maceStunSub.add(entryBuilder.startLongSlider(Text.literal("Range"), (long)(TutorialMod.CONFIG.maceAutoStunRange * 10), 0, 60)
                    .setDefaultValue(31)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceAutoStunRange = newValue / 10.0).build());
            maceStunSub.add(entryBuilder.startIntSlider(Text.literal("Back Delay"), TutorialMod.CONFIG.maceAutoStunDelay, 0, 20)
                    .setDefaultValue(1)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceAutoStunDelay = newValue).build());
            maceStunSub.add(entryBuilder.startIntSlider(Text.literal("Fail Chance"), TutorialMod.CONFIG.maceAutoStunFailChance, 0, 100)
                    .setDefaultValue(0)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceAutoStunFailChance = newValue).build());
            maceStunSub.add(entryBuilder.startIntSlider(Text.literal("Prediction Chance"), TutorialMod.CONFIG.maceAutoStunFakePredictionChance, 0, 100)
                    .setDefaultValue(0)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceAutoStunFakePredictionChance = newValue).build());
            autoStun.addEntry(maceStunSub.build());

            // Spear AutoStun SubCategory
            SubCategoryBuilder spearStunSub = entryBuilder.startSubCategory(Text.literal("Axe AutoStun (Spear)"));
            spearStunSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.spearAutoStunEnabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.spearAutoStunEnabled = newValue).build());
            spearStunSub.add(entryBuilder.startLongSlider(Text.literal("Range"), (long)(TutorialMod.CONFIG.spearAutoStunRange * 10), 0, 60)
                    .setDefaultValue(41)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.spearAutoStunRange = newValue / 10.0).build());
            spearStunSub.add(entryBuilder.startIntSlider(Text.literal("Back Delay"), TutorialMod.CONFIG.spearAutoStunDelay, 0, 20)
                    .setDefaultValue(1)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.spearAutoStunDelay = newValue).build());
            spearStunSub.add(entryBuilder.startIntSlider(Text.literal("Fail Chance"), TutorialMod.CONFIG.spearAutoStunFailChance, 0, 100)
                    .setDefaultValue(0)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.spearAutoStunFailChance = newValue).build());
            spearStunSub.add(entryBuilder.startIntSlider(Text.literal("Prediction Chance"), TutorialMod.CONFIG.spearAutoStunFakePredictionChance, 0, 100)
                    .setDefaultValue(0)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.spearAutoStunFakePredictionChance = newValue).build());
            autoStun.addEntry(spearStunSub.build());

            // Mace Attribute Swap SubCategory
            SubCategoryBuilder maceSwapSub = entryBuilder.startSubCategory(Text.literal("Mace Attribute Swap"));
            maceSwapSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.maceSwapEnabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapEnabled = newValue).build());
            maceSwapSub.add(entryBuilder.startLongSlider(Text.literal("Range"), (long)(TutorialMod.CONFIG.maceSwapRange * 10), 0, 60)
                    .setDefaultValue(31)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapRange = newValue / 10.0).build());
            maceSwapSub.add(entryBuilder.startIntSlider(Text.literal("Back Delay"), TutorialMod.CONFIG.maceSwapDelay, 0, 20)
                    .setDefaultValue(1)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapDelay = newValue).build());
            maceSwapSub.add(entryBuilder.startIntSlider(Text.literal("Fail Chance"), TutorialMod.CONFIG.maceSwapFailChance, 0, 100)
                    .setDefaultValue(0)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapFailChance = newValue).build());
            maceSwapSub.add(entryBuilder.startLongSlider(Text.literal("Min Fall Distance"), (long)(TutorialMod.CONFIG.maceSwapMinFallDistance * 10), 0, 100)
                    .setDefaultValue(30)
                    .setTooltip(Text.literal("Minimum fall distance (blocks * 10) to trigger."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapMinFallDistance = newValue / 10.0).build());
            autoStun.addEntry(maceSwapSub.build());

            // Spear Reach Swap SubCategory
            SubCategoryBuilder reachSwapSub = entryBuilder.startSubCategory(Text.literal("Spear Reach Swap"));
            reachSwapSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.spearReachSwapEnabled)
                    .setDefaultValue(true)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.spearReachSwapEnabled = newValue).build());
            reachSwapSub.add(entryBuilder.startLongSlider(Text.literal("Scan Range"), (long)(TutorialMod.CONFIG.spearReachSwapRange * 10), 0, 60)
                    .setDefaultValue(41)
                    .setTooltip(Text.literal("Maximum distance (blocks * 10) to look for targets."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.spearReachSwapRange = newValue / 10.0).build());
            reachSwapSub.add(entryBuilder.startLongSlider(Text.literal("Activation Distance"), (long)(TutorialMod.CONFIG.reachSwapActivationRange * 10), 0, 60)
                    .setDefaultValue(28)
                    .setTooltip(Text.literal("Distance (blocks * 10) beyond which to switch to Spear."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.reachSwapActivationRange = newValue / 10.0).build());
            reachSwapSub.add(entryBuilder.startIntSlider(Text.literal("Back Delay"), TutorialMod.CONFIG.reachSwapBackDelay, 0, 10)
                    .setDefaultValue(1)
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.reachSwapBackDelay = newValue).build());
            reachSwapSub.add(entryBuilder.startBooleanToggle(Text.literal("Ignore Cobwebs"), TutorialMod.CONFIG.reachSwapIgnoreCobwebs)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, Reach Swap will function even if cobwebs are blocking line of sight."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.reachSwapIgnoreCobwebs = newValue).build());
            autoStun.addEntry(reachSwapSub.build());

            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("General Fall Distance"), TutorialMod.CONFIG.minFallDistance, 1, 5)
                    .setDefaultValue(3)
                    .setTooltip(Text.literal("Global fall distance (blocks) check for other features."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.minFallDistance = newValue)
                    .build());

            // Auto Totem Category
            ConfigCategory autoTotem = builder.getOrCreateCategory(Text.literal("Auto Totem"));
            autoTotem.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto Totem Enabled"), TutorialMod.CONFIG.autoTotemEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic totem offhand replenishment feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoTotemEnabled = newValue)
                    .build());
            autoTotem.addEntry(entryBuilder.startBooleanToggle(Text.literal("Survival Mode Only"), TutorialMod.CONFIG.autoTotemSurvivalOnly)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, Auto Totem will only function while in Survival mode."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoTotemSurvivalOnly = newValue)
                    .build());
            autoTotem.addEntry(entryBuilder.startBooleanToggle(Text.literal("Refill on Totem Pop"), TutorialMod.CONFIG.autoTotemRefillOnPop)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Automatically move a totem from the designated hotbar slots to the offhand when a totem is consumed."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoTotemRefillOnPop = newValue)
                    .build());
            autoTotem.addEntry(entryBuilder.startStrField(Text.literal("Totem Hotbar Slots"),
                            TutorialMod.CONFIG.autoTotemHotbarSlots.stream().map(s -> String.valueOf(s + 1)).collect(Collectors.joining(",")))
                    .setDefaultValue("1,2,3,4,5,6,7,8,9")
                    .setTooltip(Text.literal("The hotbar slots (1-9, comma-separated) designated for holding totems."))
                    .setSaveConsumer(newValue -> {
                        try {
                            TutorialMod.CONFIG.autoTotemHotbarSlots = Arrays.stream(newValue.replace(" ", "").split(","))
                                    .map(s -> Integer.parseInt(s) - 1)
                                    .collect(Collectors.toList());
                        } catch (NumberFormatException e) {
                            // Handle invalid input, maybe log an error or keep the old value
                        }
                    })
                    .build());

            // Chat Category
            ConfigCategory chat = builder.getOrCreateCategory(Text.literal("Chat"));
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Disable Mod Chat Updates"), TutorialMod.CONFIG.disableModChatUpdates)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the mod will not send feature toggle or status updates to the chat."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.disableModChatUpdates = newValue)
                    .build());
            chat.addEntry(entryBuilder.startStrField(Text.literal("Coordinate Trigger"), TutorialMod.CONFIG.trigger)
                    .setDefaultValue("cc")
                    .setTooltip(Text.literal("The trigger word used to share coordinates in chat."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.trigger = newValue)
                    .build());
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Case Sensitive Trigger"), TutorialMod.CONFIG.caseSensitive)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the coordinate trigger word must match the exact case to function."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.caseSensitive = newValue)
                    .build());
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Replace in Chat"), TutorialMod.CONFIG.replaceInChat)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the mod will replace the trigger word with coordinates in public chat messages."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.replaceInChat = newValue)
                    .build());
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Replace in Commands"), TutorialMod.CONFIG.replaceInCommands)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the mod will replace the trigger word with coordinates in commands."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.replaceInCommands = newValue)
                    .build());
            // NOTE: The user requested to make this text box visibly wider.
            // The Cloth Config API does not provide a simple way to adjust the width of a single text field.
            // The old `startLongTextField` was removed, and `startStrField` does not have a width option.
            chat.addEntry(entryBuilder.startStrField(Text.literal("Coordinate Format"), TutorialMod.CONFIG.format)
                    .setDefaultValue("{bx} {by} {bz} {dim} {facing}")
                    .setTooltip(Text.literal("Format for coordinates. Placeholders:\n" +
                            "{x}, {y}, {z}: Precise coordinates (e.g., 123.45)\n" +
                            "{bx}, {by}, {bz}: Block coordinates (e.g., 123)\n" +
                            "{dim}: Dimension (e.g., overworld)\n" +
                            "{facing}: Cardinal direction (e.g., north)"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.format = newValue)
                    .build());

            // Macros
            for (int i = 1; i <= 5; i++) {
                final int macroNum = i;
                ModConfig.Macro macro;
                switch (macroNum) {
                    case 1: macro = TutorialMod.CONFIG.macro1; break;
                    case 2: macro = TutorialMod.CONFIG.macro2; break;
                    case 3: macro = TutorialMod.CONFIG.macro3; break;
                    case 4: macro = TutorialMod.CONFIG.macro4; break;
                    default: macro = TutorialMod.CONFIG.macro5; break;
                }

                me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder macroCategory = entryBuilder.startSubCategory(Text.literal("Macro " + macroNum));
                macroCategory.add(entryBuilder.startStrField(Text.literal("Name"), macro.name)
                    .setSaveConsumer(newValue -> macro.name = newValue).build());
                macroCategory.add(entryBuilder.startStrField(Text.literal("Hotkey"), macro.hotkey)
                    .setSaveConsumer(newValue -> macro.hotkey = newValue).build());
                macroCategory.add(entryBuilder.startStrField(Text.literal("Message/Command"), macro.message)
                    .setSaveConsumer(newValue -> macro.message = newValue).build());
                chat.addEntry(macroCategory.build());
            }

            // Hotkeys Category
            ConfigCategory hotkeys = builder.getOrCreateCategory(Text.literal("Hotkeys"));
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Open Settings Hotkey"), TutorialMod.CONFIG.openSettingsHotkey)
                    .setDefaultValue("key.keyboard.right.shift")
                    .setTooltip(Text.literal("The translation key for the hotkey used to open this configuration screen."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.openSettingsHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Master Toggle Hotkey"), TutorialMod.CONFIG.masterToggleHotkey)
                    .setDefaultValue("key.keyboard.m")
                    .setTooltip(Text.literal("The translation key for the hotkey used to toggle all mod features on or off."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.masterToggleHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Teammate Toggle Hotkey"), TutorialMod.CONFIG.teammateHotkey)
                    .setDefaultValue("key.keyboard.g")
                    .setTooltip(Text.literal("The translation key for the hotkey used to add or remove the player you are looking at from your team."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.teammateHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Trigger Bot Toggle Hotkey"), TutorialMod.CONFIG.triggerBotToggleHotkey)
                    .setDefaultValue("key.keyboard.k")
                    .setTooltip(Text.literal("The translation key for the hotkey used to toggle the Trigger Bot on or off."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotToggleHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Trigger Bot Hotkey"), TutorialMod.CONFIG.triggerBotHotkey)
                    .setDefaultValue("key.keyboard.0")
                    .setTooltip(Text.literal("The translation key for the hotkey that must be held to activate the Trigger Bot."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Overlay Toggle Hotkey"), TutorialMod.CONFIG.toggleOverlayHotkey)
                    .setDefaultValue("key.keyboard.h")
                    .setTooltip(Text.literal("The translation key for the hotkey used to toggle the overlay on or off."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.toggleOverlayHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Parkour Toggle Hotkey"), TutorialMod.CONFIG.parkourHotkey)
                    .setDefaultValue("key.keyboard.p")
                    .setTooltip(Text.literal("The translation key for the hotkey used to toggle the Parkour module."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Clutch Toggle Hotkey"), TutorialMod.CONFIG.clutchHotkey)
                    .setDefaultValue("key.keyboard.j")
                    .setTooltip(Text.literal("The translation key for the hotkey used to toggle the Clutch module."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Bridge Assist Hotkey"), TutorialMod.CONFIG.bridgeAssistHotkey)
                    .setDefaultValue("key.keyboard.left.alt")
                    .setTooltip(Text.literal("The translation key for the hotkey that must be held to activate Bridge Assist."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Mining Reset Toggle Hotkey"), TutorialMod.CONFIG.miningResetHotkey)
                    .setDefaultValue("key.keyboard.unknown")
                    .setTooltip(Text.literal("The translation key for the hotkey used to toggle the Mining Reset module."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Sprint Mode Toggle Hotkey"), TutorialMod.CONFIG.sprintModeHotkey)
                    .setDefaultValue("key.keyboard.n")
                    .setTooltip(Text.literal("The translation key for the hotkey used to toggle between Hold and Toggle sprint modes."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.sprintModeHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Sneak Mode Toggle Hotkey"), TutorialMod.CONFIG.sneakModeHotkey)
                    .setDefaultValue("key.keyboard.b")
                    .setTooltip(Text.literal("The translation key for the hotkey used to toggle between Hold and Toggle sneak modes."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.sneakModeHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startBooleanToggle(Text.literal("Hotkeys Active in Inventory"), TutorialMod.CONFIG.activeInInventory)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, hotkeys will remain functional while an inventory screen is open."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.activeInInventory = newValue)
                    .build());

            // Minecart Tech Category
            ConfigCategory minecartTech = builder.getOrCreateCategory(Text.literal("Minecart Tech"));
            minecartTech.addEntry(entryBuilder.startBooleanToggle(Text.literal("TNT Minecart Placement Enabled"), TutorialMod.CONFIG.tntMinecartPlacementEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic TNT minecart placement feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.tntMinecartPlacementEnabled = newValue)
                    .build());
            minecartTech.addEntry(entryBuilder.startBooleanToggle(Text.literal("Lava/Crossbow Sequence Enabled"), TutorialMod.CONFIG.lavaCrossbowSequenceEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the Lava/Crossbow sequence after placing a minecart."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.lavaCrossbowSequenceEnabled = newValue)
                    .build());
            minecartTech.addEntry(entryBuilder.startBooleanToggle(Text.literal("Bow Sequence Enabled"), TutorialMod.CONFIG.bowSequenceEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the Bow sequence after placing a minecart."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bowSequenceEnabled = newValue)
                    .build());
            minecartTech.addEntry(entryBuilder.startIntSlider(Text.literal("Bow Sequence Cooldown"), TutorialMod.CONFIG.bowCooldown, 0, 200)
                    .setDefaultValue(100)
                    .setTooltip(Text.literal("The cooldown (ticks) after shooting a bow before the Bow Sequence can trigger."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bowCooldown = newValue)
                    .build());

            // Movement Category
            ConfigCategory movement = builder.getOrCreateCategory(Text.literal("Movement"));

            // Parkour
            SubCategoryBuilder parkourSub = entryBuilder.startSubCategory(Text.literal("Parkour"));
            parkourSub.add(entryBuilder.startBooleanToggle(Text.literal("Parkour Enabled"), TutorialMod.CONFIG.parkourEnabled)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the player will automatically jump when about to lose ground support."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourEnabled = newValue)
                    .build());
            parkourSub.add(entryBuilder.startLongSlider(Text.literal("Parkour Prediction"), (long)(TutorialMod.CONFIG.parkourPredict * 100), 0, 50)
                    .setDefaultValue(12)
                    .setTooltip(Text.literal("How far ahead to predict ground loss. Higher values jump earlier. Default: 0.12"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourPredict = newValue / 100.0)
                    .build());
            parkourSub.add(entryBuilder.startLongSlider(Text.literal("Parkour Max Drop Height"), (long)(TutorialMod.CONFIG.parkourMaxDropHeight * 100), 0, 150)
                    .setDefaultValue(60)
                    .setTooltip(Text.literal("The maximum drop height (blocks) to ignore (prevents jumping on stairs or slabs). Default: 0.6"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourMaxDropHeight = newValue / 100.0)
                    .build());
            movement.addEntry(parkourSub.build());

            // Bridge Assist
            SubCategoryBuilder bridgeAssistSub = entryBuilder.startSubCategory(Text.literal("Bridge Assist"));
            bridgeAssistSub.add(entryBuilder.startLongSlider(Text.literal("Bridge Assist Prediction"), (long)(TutorialMod.CONFIG.bridgeAssistPredict * 100), 0, 50)
                    .setDefaultValue(16)
                    .setTooltip(Text.literal("How far ahead to predict ground loss for Bridge Assist. Default: 0.16"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistPredict = newValue / 100.0)
                    .build());
            bridgeAssistSub.add(entryBuilder.startLongSlider(Text.literal("Bridge Assist Start Height"), (long)(TutorialMod.CONFIG.bridgeAssistStartSneakHeight * 100), 0, 150)
                    .setDefaultValue(70)
                    .setTooltip(Text.literal("The drop height (blocks) required to start sneaking. Default: 0.7"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistStartSneakHeight = newValue / 100.0)
                    .build());
            bridgeAssistSub.add(entryBuilder.startLongSlider(Text.literal("Bridge Assist Stop Height"), (long)(TutorialMod.CONFIG.bridgeAssistStopSneakHeight * 100), 0, 150)
                    .setDefaultValue(50)
                    .setTooltip(Text.literal("The drop height (blocks) at which to stop sneaking. Default: 0.5"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistStopSneakHeight = newValue / 100.0)
                    .build());
            bridgeAssistSub.add(entryBuilder.startIntSlider(Text.literal("Bridge Assist Min Hold Duration"), TutorialMod.CONFIG.bridgeAssistMinHoldTicks, 0, 10)
                    .setDefaultValue(3)
                    .setTooltip(Text.literal("The minimum duration (ticks) to hold sneak after it is triggered to prevent flickering. Default: 3"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistMinHoldTicks = newValue)
                    .build());
            movement.addEntry(bridgeAssistSub.build());

            // Clutch (Water)
            SubCategoryBuilder waterClutchSub = entryBuilder.startSubCategory(Text.literal("Clutch (Water)"));
            waterClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Water Clutch Enabled"), TutorialMod.CONFIG.waterClutchEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, water buckets will be used for clutching."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.waterClutchEnabled = newValue)
                    .build());
            waterClutchSub.add(entryBuilder.startLongSlider(Text.literal("Min Fall Distance"), (long)(TutorialMod.CONFIG.clutchMinFallDistance), 0, 100)
                    .setDefaultValue(3)
                    .setTooltip(Text.literal("The minimum fall distance (blocks) required to trigger a water clutch. Default: 3"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchMinFallDistance = newValue.doubleValue())
                    .build());
            waterClutchSub.add(entryBuilder.startLongSlider(Text.literal("Activation Pitch"), (long)TutorialMod.CONFIG.clutchActivationPitch, -90, 90)
                    .setDefaultValue(60)
                    .setTooltip(Text.literal("The minimum pitch (degrees looking down) required to trigger a clutch. Default: 60"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchActivationPitch = newValue.floatValue())
                    .build());
            waterClutchSub.add(entryBuilder.startIntSlider(Text.literal("Switch Delay"), TutorialMod.CONFIG.clutchSwitchDelay, 0, 40)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The delay (ticks) to wait after fall confirmation before switching to the bucket. Default: 0"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchSwitchDelay = newValue)
                    .build());
            waterClutchSub.add(entryBuilder.startIntSlider(Text.literal("Recovery Delay"), TutorialMod.CONFIG.clutchRecoveryDelay, 0, 100)
                    .setDefaultValue(20)
                    .setTooltip(Text.literal("The delay (ticks) to wait after landing before picking up the water. Default: 20"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchRecoveryDelay = newValue)
                    .build());
            waterClutchSub.add(entryBuilder.startIntSlider(Text.literal("Restore Delay"), TutorialMod.CONFIG.clutchRestoreDelay, 0, 100)
                    .setDefaultValue(5)
                    .setTooltip(Text.literal("The delay (ticks) to wait after recovery before switching back to the original slot. Default: 5"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchRestoreDelay = newValue)
                    .build());
            waterClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Restore Original Slot"), TutorialMod.CONFIG.clutchRestoreOriginalSlot)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the mod will switch back to the original hotbar slot after a successful clutch."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchRestoreOriginalSlot = newValue)
                    .build());
            waterClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Auto Bucket Switch"), TutorialMod.CONFIG.clutchAutoSwitch)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the mod will automatically switch to a water bucket in your hotbar."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchAutoSwitch = newValue)
                    .build());
            movement.addEntry(waterClutchSub.build());

            // Clutch (Wind Charge)
            SubCategoryBuilder windClutchSub = entryBuilder.startSubCategory(Text.literal("Clutch (Wind Charge)"));
            windClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Wind Charge Clutch Enabled"), TutorialMod.CONFIG.windClutchEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, wind charges will be used for clutching."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchEnabled = newValue)
                    .build());
            windClutchSub.add(entryBuilder.startLongSlider(Text.literal("Min Fall Distance"), (long)(TutorialMod.CONFIG.windClutchMinFallDistance), 0, 200)
                    .setDefaultValue(8)
                    .setTooltip(Text.literal("The minimum fall distance (blocks) for a Wind Charge clutch. Default: 8"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchMinFallDistance = newValue.doubleValue())
                    .build());
            windClutchSub.add(entryBuilder.startIntSlider(Text.literal("Max Retry Attempts"), TutorialMod.CONFIG.windClutchMaxRetries, 0, 5)
                    .setDefaultValue(2)
                    .setTooltip(Text.literal("The number of retry attempts if the initial fire sequence fails. Default: 2"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchMaxRetries = newValue)
                    .build());
            windClutchSub.add(entryBuilder.startLongSlider(Text.literal("Success Velocity Delta"), (long)(TutorialMod.CONFIG.windClutchSuccessVyDelta * 100), 0, 200)
                    .setDefaultValue(50)
                    .setTooltip(Text.literal("The upward velocity increase required to indicate a successful clutch. Default: 0.5"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchSuccessVyDelta = newValue / 100.0)
                    .build());
            movement.addEntry(windClutchSub.build());

            movement.addEntry(entryBuilder.startBooleanToggle(Text.literal("Master Clutch Module Toggle"), TutorialMod.CONFIG.clutchEnabled)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Master toggle for all clutch features."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchEnabled = newValue)
                    .build());

            // Overlay Category
            ConfigCategory overlay = builder.getOrCreateCategory(Text.literal("Overlay"));
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Coords Overlay Enabled"), TutorialMod.CONFIG.showCoordsOverlay)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, a separate window will display player coordinates and status."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showCoordsOverlay = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Accurate Coordinates"), TutorialMod.CONFIG.showAccurateCoordinates)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, coordinates will be displayed with 3 decimal places."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showAccurateCoordinates = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Nether Coordinates"), TutorialMod.CONFIG.showNetherCoords)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the overlay will show converted coordinates (Nether if in Overworld, and vice-versa)."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showNetherCoords = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Entity Count"), TutorialMod.CONFIG.showEntityCount)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the number of loaded entities will be displayed in the overlay."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showEntityCount = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Chunk Count"), TutorialMod.CONFIG.showChunkCount)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the number of completed chunks will be displayed in the overlay."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showChunkCount = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Long Distance Coordinates"), TutorialMod.CONFIG.showLongCoords)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the overlay will display the coordinates and distance of the block or entity you are currently looking at."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showLongCoords = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startIntSlider(Text.literal("Long Distance Range"), TutorialMod.CONFIG.longCoordsMaxDistance, 64, 1024)
                    .setDefaultValue(512)
                    .setTooltip(Text.literal("The maximum distance (blocks) to scan for blocks and entities."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.longCoordsMaxDistance = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Pointing Distance"), TutorialMod.CONFIG.showLongCoordsDistance)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the distance to the pointing target will be displayed."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showLongCoordsDistance = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Detailed Cardinals"), TutorialMod.CONFIG.showDetailedCardinals)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the overlay will show abbreviated cardinal directions (NW, SE, etc.) and quadrant indicators."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showDetailedCardinals = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Sprint Status"), TutorialMod.CONFIG.showSprintModeOverlay)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the current sprint mode (Hold/Toggle) will be displayed in the overlay."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showSprintModeOverlay = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Sneak Status"), TutorialMod.CONFIG.showSneakModeOverlay)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the current sneak mode (Hold/Toggle) will be displayed in the overlay."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showSneakModeOverlay = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Latest Toggle on Overlay"), TutorialMod.CONFIG.showLatestToggleOverlay)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the latest feature toggle status will be displayed at the bottom of the overlay for 2 seconds."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showLatestToggleOverlay = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startIntSlider(Text.literal("Overlay Font Size"), TutorialMod.CONFIG.overlayFontSize, 8, 40)
                    .setDefaultValue(20)
                    .setTooltip(Text.literal("The font size of the text displayed in the overlay window."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayFontSize = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startIntSlider(Text.literal("Overlay Background Opacity"), TutorialMod.CONFIG.overlayBackgroundOpacity, 0, 255)
                    .setDefaultValue(128)
                    .setTooltip(Text.literal("The opacity level (0-255) of the overlay window background."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayBackgroundOpacity = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startStrField(Text.literal("Overlay Text Alignment"), TutorialMod.CONFIG.overlayAlignment)
                    .setDefaultValue("Left")
                    .setTooltip(Text.literal("The alignment of the text in the overlay (Left, Center, or Right)."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayAlignment = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startStrField(Text.literal("Overlay Font Name"), TutorialMod.CONFIG.overlayFontName)
                    .setDefaultValue("Consolas")
                    .setTooltip(Text.literal("The name of the font to use for the overlay text."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayFontName = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Lock Overlay"), TutorialMod.CONFIG.overlayLocked)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the overlay cannot be moved or resized, and mouse clicks will pass through it."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayLocked = newValue)
                    .build());

            // Enemy Info SubCategory
            me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder enemyInfoSubCategory = entryBuilder.startSubCategory(Text.literal("Enemy Info"));
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Enemy Info Enabled"), TutorialMod.CONFIG.showEnemyInfo)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, information about the player you are looking at will be displayed."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showEnemyInfo = newValue)
                    .build());
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Show HP Decimals"), TutorialMod.CONFIG.showHpDecimals)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, enemy health will be displayed with one decimal place."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showHpDecimals = newValue)
                    .build());
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Show Weakest Armor Piece"), TutorialMod.CONFIG.showLowestArmorPiece)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the armor piece with the lowest durability will be identified (H, C, L, B)."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showLowestArmorPiece = newValue)
                    .build());
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Extended Detection Range"), TutorialMod.CONFIG.doubleEnemyInfoRange)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the detection range for enemy info will be doubled."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.doubleEnemyInfoRange = newValue)
                    .build());
            overlay.addEntry(enemyInfoSubCategory.build());

            // Tool Switch Category
            ConfigCategory toolSwitch = builder.getOrCreateCategory(Text.literal("Tool Switch"));
            toolSwitch.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto Tool Switch Enabled"), TutorialMod.CONFIG.autoToolSwitchEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the mod will automatically switch to the most efficient tool for the current block."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchEnabled = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startBooleanToggle(Text.literal("Durability Safety Enabled"), TutorialMod.CONFIG.toolDurabilitySafetyEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the mod will stop mining if the current tool reaches 1 durability."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.toolDurabilitySafetyEnabled = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startBooleanToggle(Text.literal("Restore Original Item"), TutorialMod.CONFIG.autoToolSwitchBackEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the mod will switch back to the original item after mining is completed."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchBackEnabled = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Min Restore Delay"), TutorialMod.CONFIG.autoToolSwitchBackMinDelay, 0, 100)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The minimum delay (ticks) before restoring the original item."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchBackMinDelay = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Max Restore Delay"), TutorialMod.CONFIG.autoToolSwitchBackMaxDelay, 0, 100)
                    .setDefaultValue(5)
                    .setTooltip(Text.literal("The maximum delay (ticks) before restoring the original item."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchBackMaxDelay = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Min Mining Duration"), TutorialMod.CONFIG.autoToolSwitchMineMinDelay, 0, 100)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The minimum time (ticks) spent mining before a tool switch occurs."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchMineMinDelay = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Max Mining Duration"), TutorialMod.CONFIG.autoToolSwitchMineMaxDelay, 0, 100)
                    .setDefaultValue(2)
                    .setTooltip(Text.literal("The maximum time (ticks) spent mining before a tool switch occurs."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchMineMaxDelay = newValue)
                    .build());

            // Misc Category
            ConfigCategory misc = builder.getOrCreateCategory(Text.literal("Misc"));
            misc.addEntry(entryBuilder.startBooleanToggle(Text.literal("Click Spam Enabled"), TutorialMod.CONFIG.clickSpamEnabled)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the mod will automatically spam the use or attack key."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clickSpamEnabled = newValue)
                    .build());
            misc.addEntry(entryBuilder.startIntSlider(Text.literal("Click Spam Rate"), TutorialMod.CONFIG.clickSpamCps, 1, 20)
                    .setDefaultValue(12)
                    .setTooltip(Text.literal("The number of clicks per second (CPS) to simulate. Default: 12"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clickSpamCps = newValue)
                    .build());
            misc.addEntry(entryBuilder.startStrField(Text.literal("Click Spam Modifier Hotkey"), TutorialMod.CONFIG.clickSpamModifierKey)
                    .setDefaultValue("key.keyboard.apostrophe")
                    .setTooltip(Text.literal("The translation key for the hotkey that must be held to enable click spamming."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clickSpamModifierKey = newValue)
                    .build());
            misc.addEntry(entryBuilder.startBooleanToggle(Text.literal("Mining Reset Enabled"), TutorialMod.CONFIG.miningResetEnabled)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the mod will automatically skip the 5-tick mining cooldown when a block is broken."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetEnabled = newValue)
                    .build());
            misc.addEntry(entryBuilder.startIntSlider(Text.literal("Mining Reset Chance"), TutorialMod.CONFIG.miningResetChance, 0, 100)
                    .setDefaultValue(100)
                    .setTooltip(Text.literal("The chance (%) for the mining reset to trigger for each block broken."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetChance = newValue)
                    .build());
            misc.addEntry(entryBuilder.startBooleanToggle(Text.literal("Simulate Mining Stops"), TutorialMod.CONFIG.miningResetSimulateStops)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the mod will simulate releasing the attack key just before the block breaks for a more realistic behavior."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetSimulateStops = newValue)
                    .build());
            misc.addEntry(entryBuilder.startLongSlider(Text.literal("Early Release Threshold"), (long)(TutorialMod.CONFIG.miningResetThreshold * 100), 50, 99)
                    .setDefaultValue(92)
                    .setTooltip(Text.literal("The progress threshold (%) at which to perform the early release reset. Default: 92"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetThreshold = newValue / 100.0)
                    .build());
            misc.addEntry(entryBuilder.startIntSlider(Text.literal("Mining Reset Delay"), TutorialMod.CONFIG.miningResetDelay, 0, 5)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The cooldown duration (ticks) set after a reset occurs. 0 is fastest, 5 is vanilla. Default: 0"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetDelay = newValue)
                    .build());

            // Trigger Bot Category
            ConfigCategory triggerBot = builder.getOrCreateCategory(Text.literal("Trigger Bot"));
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Trigger Bot Enabled"), TutorialMod.CONFIG.triggerBotEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the mod will automatically attack entities when they are in your crosshair."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotEnabled = newValue)
                    .build());

            // Trigger Bot Filters SubCategory
            me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder triggerBotFilters = entryBuilder.startSubCategory(Text.literal("Filters"));
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Players"), TutorialMod.CONFIG.triggerBotIncludePlayers)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the Trigger Bot will attack other players."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludePlayers = newValue)
                    .build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Exclude Teammates"), TutorialMod.CONFIG.triggerBotExcludeTeammates)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the Trigger Bot will avoid attacking players on your team."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotExcludeTeammates = newValue)
                    .build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Hostiles"), TutorialMod.CONFIG.triggerBotIncludeHostiles)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the Trigger Bot will attack hostile mobs."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludeHostiles = newValue)
                    .build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Passives"), TutorialMod.CONFIG.triggerBotIncludePassives)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the Trigger Bot will attack passive mobs."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludePassives = newValue)
                    .build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Exclude Villagers"), TutorialMod.CONFIG.triggerBotExcludeVillagers)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the Trigger Bot will avoid attacking villagers even if passives are included."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotExcludeVillagers = newValue)
                    .build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Crystals"), TutorialMod.CONFIG.triggerBotIncludeCrystals)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the Trigger Bot will attack end crystals."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludeCrystals = newValue)
                    .build());
            triggerBot.addEntry(triggerBotFilters.build());

            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Trigger Bot Active in Inventory"), TutorialMod.CONFIG.triggerBotActiveInInventory)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the Trigger Bot will remain functional while an inventory screen is open."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotActiveInInventory = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startIntSlider(Text.literal("Min Reaction Delay"), TutorialMod.CONFIG.triggerBotReactionMinDelay, 0, 20)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The minimum duration (ticks) the crosshair must be over a target before the first attack."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotReactionMinDelay = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startIntSlider(Text.literal("Max Reaction Delay"), TutorialMod.CONFIG.triggerBotReactionMaxDelay, 0, 20)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The maximum duration (ticks) the crosshair must be over a target before the first attack."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotReactionMaxDelay = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Melee Weapons Only"), TutorialMod.CONFIG.triggerBotWeaponOnly)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, the Trigger Bot will only fire if you are holding a sword, axe, or mace."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotWeaponOnly = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Attack on Crit Only"), TutorialMod.CONFIG.attackOnCrit)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If enabled, the Trigger Bot will only attack mid-air if you are currently falling to ensure a critical hit. Grounded attacks are unaffected."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.attackOnCrit = newValue)
                    .build());

            return builder.build();
        };
    }
}
