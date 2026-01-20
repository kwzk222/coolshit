package net.rev.tutorialmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Arrays;
import java.util.stream.Collectors;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Attribute Swapping"))
                    .setDefaultBackgroundTexture(Identifier.ofVanilla("textures/gui/options_background.png"));

            builder.setSavingRunnable(TutorialMod.CONFIG::save);

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // Attribute Swapping Category
            ConfigCategory autoStun = builder.getOrCreateCategory(Text.literal("Attribute Swapping"));
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("Fake Prediction Chance (%)"), TutorialMod.CONFIG.fakePredictionChance, 0, 100)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The chance (%) to attempt an axe swap even if the enemy is not shielding."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.fakePredictionChance = newValue)
                    .build());
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("AutoStun Fail Chance (%)"), TutorialMod.CONFIG.axeSwapFailChance, 0, 100)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The chance (in %) for the regular axe swap to fail."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapFailChance = newValue)
                    .build());
            autoStun.addEntry(entryBuilder.startBooleanToggle(Text.literal("AutoStun Enabled"), TutorialMod.CONFIG.axeSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic axe swapping feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapEnabled = newValue)
                    .build());
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("AutoStun Delay"), TutorialMod.CONFIG.axeSwapDelay, 0, 20)
                    .setDefaultValue(5)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the axe."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapDelay = newValue)
                    .build());
            autoStun.addEntry(entryBuilder.startBooleanToggle(Text.literal("AutoMace Enabled"), TutorialMod.CONFIG.maceSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic mace swapping feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapEnabled = newValue)
                    .build());
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("AutoMace Delay"), TutorialMod.CONFIG.maceSwapDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the mace."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapDelay = newValue)
                    .build());
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("Axe to Original Delay"), TutorialMod.CONFIG.axeToOriginalDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the axe to the original item in a StunSlam."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeToOriginalDelay = newValue)
                    .build());
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("Mace to Original Delay"), TutorialMod.CONFIG.maceToOriginalDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the mace to the original item in a StunSlam."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceToOriginalDelay = newValue)
                    .build());
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("Minimum Fall Distance for StunSlam"), TutorialMod.CONFIG.minFallDistance, 1, 5)
                    .setDefaultValue(3)
                    .setTooltip(Text.literal("The minimum fall distance required to trigger the StunSlam."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.minFallDistance = newValue)
                    .build());

            // Auto Totem Category
            ConfigCategory autoTotem = builder.getOrCreateCategory(Text.literal("Auto Totem"));
            autoTotem.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable Auto Totem"), TutorialMod.CONFIG.autoTotemEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the Auto Totem feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoTotemEnabled = newValue)
                    .build());
            autoTotem.addEntry(entryBuilder.startBooleanToggle(Text.literal("Survival Mode Only"), TutorialMod.CONFIG.autoTotemSurvivalOnly)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("If enabled, Auto Totem will only function in Survival mode."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoTotemSurvivalOnly = newValue)
                    .build());
            autoTotem.addEntry(entryBuilder.startBooleanToggle(Text.literal("Refill on Totem Pop"), TutorialMod.CONFIG.autoTotemRefillOnPop)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Automatically switch to a totem in your hotbar and move it to your offhand when a totem pops."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoTotemRefillOnPop = newValue)
                    .build());
            autoTotem.addEntry(entryBuilder.startStrField(Text.literal("Totem Slots (1-9, comma-separated)"),
                            TutorialMod.CONFIG.autoTotemHotbarSlots.stream().map(s -> String.valueOf(s + 1)).collect(Collectors.joining(",")))
                    .setDefaultValue("1,2,3,4,5,6,7,8,9")
                    .setTooltip(Text.literal("The hotbar slots that are designated for holding totems."))
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
                    .setTooltip(Text.literal("Disables all chat messages from the mod (e.g., 'TriggerBot ON')."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.disableModChatUpdates = newValue)
                    .build());
            chat.addEntry(entryBuilder.startStrField(Text.literal("Coordinate Trigger"), TutorialMod.CONFIG.trigger)
                    .setDefaultValue("cc")
                    .setTooltip(Text.literal("The word to type to send coordinates."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.trigger = newValue)
                    .build());
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Case Sensitive Trigger"), TutorialMod.CONFIG.caseSensitive)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("If the trigger word should be case sensitive."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.caseSensitive = newValue)
                    .build());
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Replace in Chat"), TutorialMod.CONFIG.replaceInChat)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Replace the trigger word in chat messages."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.replaceInChat = newValue)
                    .build());
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Replace in Commands"), TutorialMod.CONFIG.replaceInCommands)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Replace the trigger word in commands."))
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
                    .setTooltip(Text.literal("The hotkey to open the mod settings screen."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.openSettingsHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Master Toggle Hotkey"), TutorialMod.CONFIG.masterToggleHotkey)
                    .setDefaultValue("key.keyboard.m")
                    .setTooltip(Text.literal("The hotkey to toggle the entire mod on or off. Use a translation key, e.g., 'key.keyboard.m'."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.masterToggleHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Teammate Hotkey"), TutorialMod.CONFIG.teammateHotkey)
                    .setDefaultValue("key.keyboard.g")
                    .setTooltip(Text.literal("The hotkey to add or remove a player from your teammates list. Use a translation key, e.g., 'key.keyboard.g'."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.teammateHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Trigger Bot Toggle Hotkey"), TutorialMod.CONFIG.triggerBotToggleHotkey)
                    .setDefaultValue("key.keyboard.k")
                    .setTooltip(Text.literal("The hotkey to toggle the Trigger Bot on or off. Use a translation key, e.g., 'key.keyboard.k'."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotToggleHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Trigger Bot Hotkey"), TutorialMod.CONFIG.triggerBotHotkey)
                    .setDefaultValue("key.keyboard.0")
                    .setTooltip(Text.literal("The hotkey to hold to activate the Trigger Bot. Use a translation key, e.g., 'key.keyboard.0'."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Toggle Overlay Hotkey"), TutorialMod.CONFIG.toggleOverlayHotkey)
                    .setDefaultValue("key.keyboard.h")
                    .setTooltip(Text.literal("The hotkey to toggle the overlay on or off."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.toggleOverlayHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Parkour Hotkey"), TutorialMod.CONFIG.parkourHotkey)
                    .setDefaultValue("key.keyboard.p")
                    .setTooltip(Text.literal("The hotkey to toggle the Parkour module."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Clutch Hotkey"), TutorialMod.CONFIG.clutchHotkey)
                    .setDefaultValue("key.keyboard.j")
                    .setTooltip(Text.literal("The hotkey to toggle the Clutch module."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Bridge Assist Hotkey"), TutorialMod.CONFIG.bridgeAssistHotkey)
                    .setDefaultValue("key.keyboard.left.alt")
                    .setTooltip(Text.literal("The hotkey to hold to activate the Bridge Assist module."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Mining Reset Toggle Hotkey"), TutorialMod.CONFIG.miningResetHotkey)
                    .setDefaultValue("key.keyboard.unknown")
                    .setTooltip(Text.literal("The hotkey to toggle the Mining Reset module."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Sprint Mode Toggle Hotkey"), TutorialMod.CONFIG.sprintModeHotkey)
                    .setDefaultValue("key.keyboard.n")
                    .setTooltip(Text.literal("The hotkey to toggle between Hold and Toggle sprint modes."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.sprintModeHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Sneak Mode Toggle Hotkey"), TutorialMod.CONFIG.sneakModeHotkey)
                    .setDefaultValue("key.keyboard.b")
                    .setTooltip(Text.literal("The hotkey to toggle between Hold and Toggle sneak modes."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.sneakModeHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startBooleanToggle(Text.literal("Active in Inventory"), TutorialMod.CONFIG.activeInInventory)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Whether the hotkeys should be active while you are in an inventory screen."))
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
            minecartTech.addEntry(entryBuilder.startIntSlider(Text.literal("Bow Cooldown"), TutorialMod.CONFIG.bowCooldown, 0, 200)
                    .setDefaultValue(100)
                    .setTooltip(Text.literal("The cooldown in ticks after shooting a bow before the Bow Sequence can trigger."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bowCooldown = newValue)
                    .build());

            // Movement Category
            ConfigCategory movement = builder.getOrCreateCategory(Text.literal("Movement"));

            // Parkour
            SubCategoryBuilder parkourSub = entryBuilder.startSubCategory(Text.literal("Parkour"));
            parkourSub.add(entryBuilder.startBooleanToggle(Text.literal("Parkour Enabled"), TutorialMod.CONFIG.parkourEnabled)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Automatically jump when about to lose ground support."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourEnabled = newValue)
                    .build());
            parkourSub.add(entryBuilder.startLongSlider(Text.literal("Parkour Prediction"), (long)(TutorialMod.CONFIG.parkourPredict * 100), 0, 50)
                    .setDefaultValue(12)
                    .setTooltip(Text.literal("How far ahead to predict ground loss. Higher values jump earlier. Default: 0.12 (12 on slider)"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourPredict = newValue / 100.0)
                    .build());
            parkourSub.add(entryBuilder.startLongSlider(Text.literal("Parkour Max Drop Height"), (long)(TutorialMod.CONFIG.parkourMaxDropHeight * 100), 0, 150)
                    .setDefaultValue(60)
                    .setTooltip(Text.literal("Maximum drop height to ignore (prevents jumping on stairs/slabs). Default: 0.6 (60 on slider)"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourMaxDropHeight = newValue / 100.0)
                    .build());
            movement.addEntry(parkourSub.build());

            // Bridge Assist
            SubCategoryBuilder bridgeAssistSub = entryBuilder.startSubCategory(Text.literal("Bridge Assist"));
            bridgeAssistSub.add(entryBuilder.startLongSlider(Text.literal("Bridge Assist Prediction"), (long)(TutorialMod.CONFIG.bridgeAssistPredict * 100), 0, 50)
                    .setDefaultValue(16)
                    .setTooltip(Text.literal("How far ahead to predict ground loss for Bridge Assist. Default: 0.16 (16 on slider)"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistPredict = newValue / 100.0)
                    .build());
            bridgeAssistSub.add(entryBuilder.startLongSlider(Text.literal("Bridge Assist Start Height"), (long)(TutorialMod.CONFIG.bridgeAssistStartSneakHeight * 100), 0, 150)
                    .setDefaultValue(70)
                    .setTooltip(Text.literal("Start sneaking when drop is above this height. Default: 0.70 (70 on slider)"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistStartSneakHeight = newValue / 100.0)
                    .build());
            bridgeAssistSub.add(entryBuilder.startLongSlider(Text.literal("Bridge Assist Stop Height"), (long)(TutorialMod.CONFIG.bridgeAssistStopSneakHeight * 100), 0, 150)
                    .setDefaultValue(50)
                    .setTooltip(Text.literal("Stop sneaking only when drop is below this height. Default: 0.50 (50 on slider)"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistStopSneakHeight = newValue / 100.0)
                    .build());
            bridgeAssistSub.add(entryBuilder.startIntSlider(Text.literal("Bridge Assist Min Hold Ticks"), TutorialMod.CONFIG.bridgeAssistMinHoldTicks, 0, 10)
                    .setDefaultValue(3)
                    .setTooltip(Text.literal("Minimum number of ticks to hold sneak after it is triggered. Prevents flickering. Default: 3"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistMinHoldTicks = newValue)
                    .build());
            movement.addEntry(bridgeAssistSub.build());

            // Clutch (Water)
            SubCategoryBuilder waterClutchSub = entryBuilder.startSubCategory(Text.literal("Clutch (Water)"));
            waterClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Enable Water Clutch"), TutorialMod.CONFIG.waterClutchEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Whether to use water buckets for clutching."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.waterClutchEnabled = newValue)
                    .build());
            waterClutchSub.add(entryBuilder.startLongSlider(Text.literal("Min Fall Distance (Water)"), (long)(TutorialMod.CONFIG.clutchMinFallDistance), 0, 100)
                    .setDefaultValue(3)
                    .setTooltip(Text.literal("Minimum fall distance to trigger water clutch. Default: 3 blocks"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchMinFallDistance = newValue.doubleValue())
                    .build());
            waterClutchSub.add(entryBuilder.startLongSlider(Text.literal("Activation Pitch"), (long)TutorialMod.CONFIG.clutchActivationPitch, -90, 90)
                    .setDefaultValue(60)
                    .setTooltip(Text.literal("Minimum pitch (looking down) to trigger clutch. Default: 60"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchActivationPitch = newValue.floatValue())
                    .build());
            waterClutchSub.add(entryBuilder.startIntSlider(Text.literal("Switch Delay (ticks)"), TutorialMod.CONFIG.clutchSwitchDelay, 0, 40)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("Time to wait after fall confirmation before switching to bucket. Default: 0"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchSwitchDelay = newValue)
                    .build());
            waterClutchSub.add(entryBuilder.startIntSlider(Text.literal("Recovery Delay (ticks)"), TutorialMod.CONFIG.clutchRecoveryDelay, 0, 100)
                    .setDefaultValue(20)
                    .setTooltip(Text.literal("Time to wait after landing before picking up water. Default: 20"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchRecoveryDelay = newValue)
                    .build());
            waterClutchSub.add(entryBuilder.startIntSlider(Text.literal("Restore Delay (ticks)"), TutorialMod.CONFIG.clutchRestoreDelay, 0, 100)
                    .setDefaultValue(5)
                    .setTooltip(Text.literal("Time to wait after recovery before switching back to original slot. Default: 5"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchRestoreDelay = newValue)
                    .build());
            waterClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Restore Original Slot"), TutorialMod.CONFIG.clutchRestoreOriginalSlot)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Whether to switch back to the original slot after clutching."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchRestoreOriginalSlot = newValue)
                    .build());
            waterClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Auto Bucket Switch"), TutorialMod.CONFIG.clutchAutoSwitch)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Whether to automatically switch to the water bucket."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchAutoSwitch = newValue)
                    .build());
            movement.addEntry(waterClutchSub.build());

            // Clutch (Wind Charge)
            SubCategoryBuilder windClutchSub = entryBuilder.startSubCategory(Text.literal("Clutch (Wind Charge)"));
            windClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Enable Wind Clutch"), TutorialMod.CONFIG.windClutchEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Whether to use wind charges for clutching."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchEnabled = newValue)
                    .build());
            windClutchSub.add(entryBuilder.startLongSlider(Text.literal("Min Fall Distance (Wind)"), (long)(TutorialMod.CONFIG.windClutchMinFallDistance), 0, 200)
                    .setDefaultValue(8)
                    .setTooltip(Text.literal("Minimum fall distance for Wind Charge clutch. Default: 8 blocks"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchMinFallDistance = newValue.doubleValue())
                    .build());
            windClutchSub.add(entryBuilder.startIntSlider(Text.literal("Wind Clutch Fire Ticks"), TutorialMod.CONFIG.windClutchFireTicks, 1, 20)
                    .setDefaultValue(6)
                    .setTooltip(Text.literal("Fire when estimated ticks-to-impact <= this. Default: 6"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchFireTicks = newValue)
                    .build());
            windClutchSub.add(entryBuilder.startIntSlider(Text.literal("High Fall Fire Ticks"), TutorialMod.CONFIG.windClutchHighFallFireTicks, 1, 20)
                    .setDefaultValue(3)
                    .setTooltip(Text.literal("Fire ticks used when falling from more than 170 blocks. Default: 3"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchHighFallFireTicks = newValue)
                    .build());
            windClutchSub.add(entryBuilder.startIntSlider(Text.literal("Wind Clutch Max Retries"), TutorialMod.CONFIG.windClutchMaxRetries, 0, 5)
                    .setDefaultValue(2)
                    .setTooltip(Text.literal("Retry attempts if first fire didn't succeed. Default: 2"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchMaxRetries = newValue)
                    .build());
            windClutchSub.add(entryBuilder.startLongSlider(Text.literal("Wind Clutch Success Vy Delta"), (long)(TutorialMod.CONFIG.windClutchSuccessVyDelta * 100), 0, 200)
                    .setDefaultValue(50)
                    .setTooltip(Text.literal("Upward velocity increase indicating success. Default: 0.5 (50 on slider)"))
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
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Coords Overlay"), TutorialMod.CONFIG.showCoordsOverlay)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Shows a separate window with your coordinates."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showCoordsOverlay = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Accurate Coordinates"), TutorialMod.CONFIG.showAccurateCoordinates)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Show coordinates with 3 decimal places."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showAccurateCoordinates = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Entity Count"), TutorialMod.CONFIG.showEntityCount)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Show the entity count in the overlay."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showEntityCount = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Sprint Mode Status"), TutorialMod.CONFIG.showSprintModeOverlay)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Show the current sprint mode (Hold/Toggle) in the overlay."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showSprintModeOverlay = newValue)
                    .build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Sneak Mode Status"), TutorialMod.CONFIG.showSneakModeOverlay)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Show the current sneak mode (Hold/Toggle) in the overlay."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showSneakModeOverlay = newValue)
                    .build());

            // Enemy Info SubCategory
            me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder enemyInfoSubCategory = entryBuilder.startSubCategory(Text.literal("Enemy Info"));
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Show Enemy Info"), TutorialMod.CONFIG.showEnemyInfo)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Shows information about the player you are looking at."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showEnemyInfo = newValue)
                    .build());
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Show HP Decimals"), TutorialMod.CONFIG.showHpDecimals)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Show one decimal place for enemy HP."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showHpDecimals = newValue)
                    .build());
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Show Lowest Armor Piece"), TutorialMod.CONFIG.showLowestArmorPiece)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Show which armor piece has the lowest durability (H, C, L, B)."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.showLowestArmorPiece = newValue)
                    .build());
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Double Enemy Info Range"), TutorialMod.CONFIG.doubleEnemyInfoRange)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Doubles the range at which enemy info is displayed."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.doubleEnemyInfoRange = newValue)
                    .build());
            overlay.addEntry(enemyInfoSubCategory.build());

            // Tool Switch Category
            ConfigCategory toolSwitch = builder.getOrCreateCategory(Text.literal("Tool Switch"));
            toolSwitch.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto Tool Switch"), TutorialMod.CONFIG.autoToolSwitchEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Automatically switches to the correct tool when breaking a block."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchEnabled = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startBooleanToggle(Text.literal("Tool Durability Safety"), TutorialMod.CONFIG.toolDurabilitySafetyEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Stops mining when the tool has 1 durability left."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.toolDurabilitySafetyEnabled = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startBooleanToggle(Text.literal("Switch Back to Original Item"), TutorialMod.CONFIG.autoToolSwitchBackEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Switches back to the original item after you stop mining."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchBackEnabled = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Min Switch Back Delay (ticks)"), TutorialMod.CONFIG.autoToolSwitchBackMinDelay, 0, 100)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The minimum delay in ticks before switching back to the original item."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchBackMinDelay = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Max Switch Back Delay (ticks)"), TutorialMod.CONFIG.autoToolSwitchBackMaxDelay, 0, 100)
                    .setDefaultValue(5)
                    .setTooltip(Text.literal("The maximum delay in ticks before switching back to the original item."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchBackMaxDelay = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Min Mining Time (ticks)"), TutorialMod.CONFIG.autoToolSwitchMineMinDelay, 0, 100)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The minimum time in ticks you need to be mining a block before the tool switches."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchMineMinDelay = newValue)
                    .build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Max Mining Time (ticks)"), TutorialMod.CONFIG.autoToolSwitchMineMaxDelay, 0, 100)
                    .setDefaultValue(2)
                    .setTooltip(Text.literal("The maximum time in ticks you need to be mining a block before the tool switches."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchMineMaxDelay = newValue)
                    .build());

            // Misc Category
            ConfigCategory misc = builder.getOrCreateCategory(Text.literal("Misc"));
            misc.addEntry(entryBuilder.startBooleanToggle(Text.literal("Click Spam Enabled"), TutorialMod.CONFIG.clickSpamEnabled)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Enable or disable the Click Spam feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clickSpamEnabled = newValue)
                    .build());
            misc.addEntry(entryBuilder.startIntSlider(Text.literal("Click Spam CPS"), TutorialMod.CONFIG.clickSpamCps, 1, 20)
                    .setDefaultValue(12)
                    .setTooltip(Text.literal("Spam rate for LMB/RMB while holding the modifier key. Default: 12"))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clickSpamCps = newValue)
                    .build());
            misc.addEntry(entryBuilder.startStrField(Text.literal("Click Spam Modifier Hotkey"), TutorialMod.CONFIG.clickSpamModifierKey)
                    .setDefaultValue("key.keyboard.apostrophe")
                    .setTooltip(Text.literal("The key to hold to enable click spamming. Use translation key (e.g., 'key.keyboard.apostrophe')."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.clickSpamModifierKey = newValue)
                    .build());
            misc.addEntry(entryBuilder.startBooleanToggle(Text.literal("Mining Reset Enabled"), TutorialMod.CONFIG.miningResetEnabled)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Automatically skips the 5-tick mining cooldown by briefly releasing and re-pressing the attack key when a block is broken."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetEnabled = newValue)
                    .build());

            // Trigger Bot Category
            ConfigCategory triggerBot = builder.getOrCreateCategory(Text.literal("Trigger Bot"));
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable Trigger Bot"), TutorialMod.CONFIG.triggerBotEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the Trigger Bot feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotEnabled = newValue)
                    .build());

            // Trigger Bot Filters SubCategory
            me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder triggerBotFilters = entryBuilder.startSubCategory(Text.literal("Filters"));
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Players"), TutorialMod.CONFIG.triggerBotIncludePlayers)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Attack players."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludePlayers = newValue)
                    .build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Exclude Teammates"), TutorialMod.CONFIG.triggerBotExcludeTeammates)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Avoid attacking teammates."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotExcludeTeammates = newValue)
                    .build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Hostiles"), TutorialMod.CONFIG.triggerBotIncludeHostiles)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Attack hostile mobs."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludeHostiles = newValue)
                    .build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Passives"), TutorialMod.CONFIG.triggerBotIncludePassives)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Attack passive mobs."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludePassives = newValue)
                    .build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Exclude Villagers"), TutorialMod.CONFIG.triggerBotExcludeVillagers)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Avoid attacking villagers even if passives are included."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotExcludeVillagers = newValue)
                    .build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Crystals"), TutorialMod.CONFIG.triggerBotIncludeCrystals)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Attack end crystals."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludeCrystals = newValue)
                    .build());
            triggerBot.addEntry(triggerBotFilters.build());

            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Active in Inventory"), TutorialMod.CONFIG.triggerBotActiveInInventory)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Whether the Trigger Bot should be active while you are in an inventory screen."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotActiveInInventory = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startIntSlider(Text.literal("Min Delay (ticks)"), TutorialMod.CONFIG.triggerBotMinDelay, 0, 100)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The minimum delay in ticks before attacking."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotMinDelay = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startIntSlider(Text.literal("Max Delay (ticks)"), TutorialMod.CONFIG.triggerBotMaxDelay, 0, 100)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("The maximum delay in ticks before attacking."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotMaxDelay = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Attack on Crit"), TutorialMod.CONFIG.attackOnCrit)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Only attack when the player is on the ground or falling."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.attackOnCrit = newValue)
                    .build());

            return builder.build();
        };
    }
}
