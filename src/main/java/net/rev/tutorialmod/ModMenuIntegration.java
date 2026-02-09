package net.rev.tutorialmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.rev.tutorialmod.mixin.ScreenAccessor;

import java.util.Arrays;
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
            builder.setDoesConfirmSave(false);

            builder.setAfterInitConsumer(screen -> {
                int buttonWidth = 60;
                int buttonHeight = 20;
                int x = screen.width - buttonWidth - 10;
                int y = screen.height - 26;

                ButtonWidget saveButton = ButtonWidget.builder(Text.literal("Save"), button -> {
                    TutorialMod.CONFIG.save();
                    TutorialMod.sendUpdateMessage("Settings saved successfully.");
                }).dimensions(x, y, buttonWidth, buttonHeight).build();

                ((ScreenAccessor) screen).invokeAddDrawableChild(saveButton);
            });

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // 1. Attribute Swapping
            ConfigCategory autoStun = builder.getOrCreateCategory(Text.literal("Attribute Swapping"));
            autoStun.addEntry(entryBuilder.startBooleanToggle(Text.literal("Facing Check Enabled"), TutorialMod.CONFIG.autoStunFacingCheck).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoStunFacingCheck = newValue).build());
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("Axe to Original Delay"), TutorialMod.CONFIG.axeToOriginalDelay, 0, 20).setDefaultValue(1).setSaveConsumer(newValue -> TutorialMod.CONFIG.axeToOriginalDelay = newValue).build());
            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("Mace to Original Delay"), TutorialMod.CONFIG.maceToOriginalDelay, 0, 20).setDefaultValue(1).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceToOriginalDelay = newValue).build());

            SubCategoryBuilder axeStunSub = entryBuilder.startSubCategory(Text.literal("Axe AutoStun (Sword)"));
            axeStunSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.axeSwapEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapEnabled = newValue).build());
            axeStunSub.add(entryBuilder.startLongSlider(Text.literal("Range"), (long)(TutorialMod.CONFIG.axeSwapRange * 10), 0, 60).setDefaultValue(31).setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapRange = newValue / 10.0).build());
            axeStunSub.add(entryBuilder.startIntSlider(Text.literal("Back Delay"), TutorialMod.CONFIG.axeSwapDelay, 0, 20).setDefaultValue(1).setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapDelay = newValue).build());
            axeStunSub.add(entryBuilder.startIntSlider(Text.literal("Fail Chance"), TutorialMod.CONFIG.axeSwapFailChance, 0, 100).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapFailChance = newValue).build());
            axeStunSub.add(entryBuilder.startIntSlider(Text.literal("Prediction Chance"), TutorialMod.CONFIG.axeSwapFakePredictionChance, 0, 100).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapFakePredictionChance = newValue).build());
            autoStun.addEntry(axeStunSub.build());

            SubCategoryBuilder maceStunSub = entryBuilder.startSubCategory(Text.literal("Axe AutoStun (Mace)"));
            maceStunSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.maceAutoStunEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceAutoStunEnabled = newValue).build());
            maceStunSub.add(entryBuilder.startLongSlider(Text.literal("Range"), (long)(TutorialMod.CONFIG.maceAutoStunRange * 10), 0, 60).setDefaultValue(31).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceAutoStunRange = newValue / 10.0).build());
            maceStunSub.add(entryBuilder.startIntSlider(Text.literal("Back Delay"), TutorialMod.CONFIG.maceAutoStunDelay, 0, 20).setDefaultValue(1).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceAutoStunDelay = newValue).build());
            maceStunSub.add(entryBuilder.startIntSlider(Text.literal("Fail Chance"), TutorialMod.CONFIG.maceAutoStunFailChance, 0, 100).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceAutoStunFailChance = newValue).build());
            maceStunSub.add(entryBuilder.startIntSlider(Text.literal("Prediction Chance"), TutorialMod.CONFIG.maceAutoStunFakePredictionChance, 0, 100).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceAutoStunFakePredictionChance = newValue).build());
            autoStun.addEntry(maceStunSub.build());

            SubCategoryBuilder spearStunSub = entryBuilder.startSubCategory(Text.literal("Axe AutoStun (Spear)"));
            spearStunSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.spearAutoStunEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.spearAutoStunEnabled = newValue).build());
            spearStunSub.add(entryBuilder.startLongSlider(Text.literal("Range"), (long)(TutorialMod.CONFIG.spearAutoStunRange * 10), 0, 60).setDefaultValue(41).setSaveConsumer(newValue -> TutorialMod.CONFIG.spearAutoStunRange = newValue / 10.0).build());
            spearStunSub.add(entryBuilder.startIntSlider(Text.literal("Back Delay"), TutorialMod.CONFIG.spearAutoStunDelay, 0, 20).setDefaultValue(1).setSaveConsumer(newValue -> TutorialMod.CONFIG.spearAutoStunDelay = newValue).build());
            spearStunSub.add(entryBuilder.startIntSlider(Text.literal("Fail Chance"), TutorialMod.CONFIG.spearAutoStunFailChance, 0, 100).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.spearAutoStunFailChance = newValue).build());
            spearStunSub.add(entryBuilder.startIntSlider(Text.literal("Prediction Chance"), TutorialMod.CONFIG.spearAutoStunFakePredictionChance, 0, 100).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.spearAutoStunFakePredictionChance = newValue).build());
            autoStun.addEntry(spearStunSub.build());

            SubCategoryBuilder maceSwapSub = entryBuilder.startSubCategory(Text.literal("Mace Attribute Swap"));
            maceSwapSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.maceSwapEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapEnabled = newValue).build());
            maceSwapSub.add(entryBuilder.startLongSlider(Text.literal("Range"), (long)(TutorialMod.CONFIG.maceSwapRange * 10), 0, 60).setDefaultValue(31).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapRange = newValue / 10.0).build());
            maceSwapSub.add(entryBuilder.startIntSlider(Text.literal("Back Delay"), TutorialMod.CONFIG.maceSwapDelay, 0, 20).setDefaultValue(1).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapDelay = newValue).build());
            maceSwapSub.add(entryBuilder.startIntSlider(Text.literal("Fail Chance"), TutorialMod.CONFIG.maceSwapFailChance, 0, 100).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapFailChance = newValue).build());
            maceSwapSub.add(entryBuilder.startLongSlider(Text.literal("Min Fall Distance"), (long)(TutorialMod.CONFIG.maceSwapMinFallDistance * 10), 0, 100).setDefaultValue(30).setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapMinFallDistance = newValue / 10.0).build());
            autoStun.addEntry(maceSwapSub.build());

            SubCategoryBuilder reachSwapSub = entryBuilder.startSubCategory(Text.literal("Spear Reach Swap"));
            reachSwapSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.spearReachSwapEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.spearReachSwapEnabled = newValue).build());
            reachSwapSub.add(entryBuilder.startLongSlider(Text.literal("Scan Range"), (long)(TutorialMod.CONFIG.spearReachSwapRange * 10), 0, 60).setDefaultValue(41).setSaveConsumer(newValue -> TutorialMod.CONFIG.spearReachSwapRange = newValue / 10.0).build());
            reachSwapSub.add(entryBuilder.startLongSlider(Text.literal("Activation Distance"), (long)(TutorialMod.CONFIG.reachSwapActivationRange * 10), 0, 60).setDefaultValue(28).setSaveConsumer(newValue -> TutorialMod.CONFIG.reachSwapActivationRange = newValue / 10.0).build());
            reachSwapSub.add(entryBuilder.startIntSlider(Text.literal("Back Delay"), TutorialMod.CONFIG.reachSwapBackDelay, 0, 10).setDefaultValue(1).setSaveConsumer(newValue -> TutorialMod.CONFIG.reachSwapBackDelay = newValue).build());
            reachSwapSub.add(entryBuilder.startBooleanToggle(Text.literal("Ignore Cobwebs"), TutorialMod.CONFIG.reachSwapIgnoreCobwebs).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.reachSwapIgnoreCobwebs = newValue).build());
            autoStun.addEntry(reachSwapSub.build());

            autoStun.addEntry(entryBuilder.startIntSlider(Text.literal("General Fall Distance"), TutorialMod.CONFIG.minFallDistance, 1, 5).setDefaultValue(3).setSaveConsumer(newValue -> TutorialMod.CONFIG.minFallDistance = newValue).build());

            // 2. Crystal/Mace (Auto Totem)
            ConfigCategory autoTotem = builder.getOrCreateCategory(Text.literal("Crystal/Mace"));
            autoTotem.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto Totem Enabled"), TutorialMod.CONFIG.autoTotemEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoTotemEnabled = newValue).build());
            autoTotem.addEntry(entryBuilder.startBooleanToggle(Text.literal("Survival Mode Only"), TutorialMod.CONFIG.autoTotemSurvivalOnly).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoTotemSurvivalOnly = newValue).build());
            autoTotem.addEntry(entryBuilder.startBooleanToggle(Text.literal("Refill on Totem Pop"), TutorialMod.CONFIG.autoTotemRefillOnPop).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoTotemRefillOnPop = newValue).build());
            autoTotem.addEntry(entryBuilder.startStrField(Text.literal("Totem Hotbar Slots"), TutorialMod.CONFIG.autoTotemHotbarSlots.stream().map(s -> String.valueOf(s + 1)).collect(Collectors.joining(","))).setDefaultValue("1,2,3,4,5,6,7,8,9").setSaveConsumer(newValue -> {
                try { TutorialMod.CONFIG.autoTotemHotbarSlots = Arrays.stream(newValue.replace(" ", "").split(",")).map(s -> Integer.parseInt(s) - 1).collect(Collectors.toList()); } catch (NumberFormatException ignored) {}
            }).build());

            // 3. Movement
            ConfigCategory movement = builder.getOrCreateCategory(Text.literal("Movement"));
            SubCategoryBuilder parkourSub = entryBuilder.startSubCategory(Text.literal("Parkour"));
            parkourSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.parkourEnabled).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourEnabled = newValue).build());
            parkourSub.add(entryBuilder.startStrField(Text.literal("Hotkey"), TutorialMod.CONFIG.parkourHotkey).setDefaultValue("key.keyboard.p").setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourHotkey = newValue).build());
            parkourSub.add(entryBuilder.startLongSlider(Text.literal("Prediction"), (long)(TutorialMod.CONFIG.parkourPredict * 100), 0, 50).setDefaultValue(12).setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourPredict = newValue / 100.0).build());
            parkourSub.add(entryBuilder.startLongSlider(Text.literal("Max Drop Height"), (long)(TutorialMod.CONFIG.parkourMaxDropHeight * 100), 0, 150).setDefaultValue(60).setSaveConsumer(newValue -> TutorialMod.CONFIG.parkourMaxDropHeight = newValue / 100.0).build());
            movement.addEntry(parkourSub.build());

            SubCategoryBuilder bridgeAssistSub = entryBuilder.startSubCategory(Text.literal("Bridge Assist"));
            bridgeAssistSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.bridgeAssistEnabled).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistEnabled = newValue).build());
            bridgeAssistSub.add(entryBuilder.startStrField(Text.literal("Hotkey"), TutorialMod.CONFIG.bridgeAssistHotkey).setDefaultValue("key.keyboard.left.alt").setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistHotkey = newValue).build());
            bridgeAssistSub.add(entryBuilder.startLongSlider(Text.literal("Prediction"), (long)(TutorialMod.CONFIG.bridgeAssistPredict * 100), 0, 50).setDefaultValue(16).setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistPredict = newValue / 100.0).build());
            bridgeAssistSub.add(entryBuilder.startLongSlider(Text.literal("Start Height"), (long)(TutorialMod.CONFIG.bridgeAssistStartSneakHeight * 100), 0, 150).setDefaultValue(70).setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistStartSneakHeight = newValue / 100.0).build());
            bridgeAssistSub.add(entryBuilder.startLongSlider(Text.literal("Stop Height"), (long)(TutorialMod.CONFIG.bridgeAssistStopSneakHeight * 100), 0, 150).setDefaultValue(50).setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistStopSneakHeight = newValue / 100.0).build());
            bridgeAssistSub.add(entryBuilder.startIntSlider(Text.literal("Min Hold Duration"), TutorialMod.CONFIG.bridgeAssistMinHoldTicks, 0, 10).setDefaultValue(3).setSaveConsumer(newValue -> TutorialMod.CONFIG.bridgeAssistMinHoldTicks = newValue).build());
            movement.addEntry(bridgeAssistSub.build());

            SubCategoryBuilder waterClutchSub = entryBuilder.startSubCategory(Text.literal("Clutch (Water)"));
            waterClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.waterClutchEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.waterClutchEnabled = newValue).build());
            waterClutchSub.add(entryBuilder.startStrField(Text.literal("Hotkey"), TutorialMod.CONFIG.clutchHotkey).setDefaultValue("key.keyboard.j").setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchHotkey = newValue).build());
            waterClutchSub.add(entryBuilder.startLongSlider(Text.literal("Min Fall Distance"), (long)(TutorialMod.CONFIG.clutchMinFallDistance), 0, 100).setDefaultValue(3).setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchMinFallDistance = newValue.doubleValue()).build());
            waterClutchSub.add(entryBuilder.startLongSlider(Text.literal("Activation Pitch"), (long)TutorialMod.CONFIG.clutchActivationPitch, -90, 90).setDefaultValue(60).setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchActivationPitch = newValue.floatValue()).build());
            waterClutchSub.add(entryBuilder.startIntSlider(Text.literal("Switch Delay"), TutorialMod.CONFIG.clutchSwitchDelay, 0, 40).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchSwitchDelay = newValue).build());
            waterClutchSub.add(entryBuilder.startIntSlider(Text.literal("Recovery Delay"), TutorialMod.CONFIG.clutchRecoveryDelay, 0, 100).setDefaultValue(20).setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchRecoveryDelay = newValue).build());
            waterClutchSub.add(entryBuilder.startIntSlider(Text.literal("Restore Delay"), TutorialMod.CONFIG.clutchRestoreDelay, 0, 100).setDefaultValue(5).setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchRestoreDelay = newValue).build());
            waterClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Restore Original Slot"), TutorialMod.CONFIG.clutchRestoreOriginalSlot).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchRestoreOriginalSlot = newValue).build());
            waterClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Auto Bucket Switch"), TutorialMod.CONFIG.clutchAutoSwitch).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchAutoSwitch = newValue).build());
            movement.addEntry(waterClutchSub.build());

            SubCategoryBuilder windClutchSub = entryBuilder.startSubCategory(Text.literal("Clutch (Wind Charge)"));
            windClutchSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.windClutchEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchEnabled = newValue).build());
            windClutchSub.add(entryBuilder.startLongSlider(Text.literal("Min Fall Distance"), (long)(TutorialMod.CONFIG.windClutchMinFallDistance), 0, 200).setDefaultValue(8).setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchMinFallDistance = newValue.doubleValue()).build());
            windClutchSub.add(entryBuilder.startIntSlider(Text.literal("Max Retry Attempts"), TutorialMod.CONFIG.windClutchMaxRetries, 0, 5).setDefaultValue(2).setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchMaxRetries = newValue).build());
            windClutchSub.add(entryBuilder.startLongSlider(Text.literal("Success Velocity Delta"), (long)(TutorialMod.CONFIG.windClutchSuccessVyDelta * 100), 0, 200).setDefaultValue(50).setSaveConsumer(newValue -> TutorialMod.CONFIG.windClutchSuccessVyDelta = newValue / 100.0).build());
            movement.addEntry(windClutchSub.build());

            movement.addEntry(entryBuilder.startBooleanToggle(Text.literal("Master Clutch Module Toggle"), TutorialMod.CONFIG.clutchEnabled).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.clutchEnabled = newValue).build());

            SubCategoryBuilder jumpResetSub = entryBuilder.startSubCategory(Text.literal("Jump Reset"));
            jumpResetSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.jumpResetEnabled).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.jumpResetEnabled = newValue).build());
            jumpResetSub.add(entryBuilder.startStrField(Text.literal("Hotkey"), TutorialMod.CONFIG.jumpResetHotkey).setDefaultValue("key.keyboard.b").setSaveConsumer(newValue -> TutorialMod.CONFIG.jumpResetHotkey = newValue).build());
            jumpResetSub.add(entryBuilder.startIntSlider(Text.literal("Delay"), TutorialMod.CONFIG.jumpResetDelay, 0, 10).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.jumpResetDelay = newValue).build());
            jumpResetSub.add(entryBuilder.startIntSlider(Text.literal("Fail Chance (%)"), TutorialMod.CONFIG.jumpResetFailChance, 0, 100).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.jumpResetFailChance = newValue).build());
            jumpResetSub.add(entryBuilder.startIntSlider(Text.literal("Max Extra Fail Delay"), TutorialMod.CONFIG.jumpResetMaxExtraDelay, 1, 20).setDefaultValue(5).setSaveConsumer(newValue -> TutorialMod.CONFIG.jumpResetMaxExtraDelay = newValue).build());
            movement.addEntry(jumpResetSub.build());

            // 4. Potions
            ConfigCategory potionModule = builder.getOrCreateCategory(Text.literal("Potions"));
            potionModule.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.potionModuleEnabled).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.potionModuleEnabled = newValue).build());
            potionModule.addEntry(entryBuilder.startStrField(Text.literal("Hotkey"), TutorialMod.CONFIG.potionHotkey).setDefaultValue("key.keyboard.left.alt").setSaveConsumer(newValue -> TutorialMod.CONFIG.potionHotkey = newValue).build());
            potionModule.addEntry(entryBuilder.startLongSlider(Text.literal("Activation Pitch"), (long)TutorialMod.CONFIG.potionActivationPitch, 0, 90).setDefaultValue(60).setSaveConsumer(newValue -> TutorialMod.CONFIG.potionActivationPitch = newValue.doubleValue()).build());
            potionModule.addEntry(entryBuilder.startLongSlider(Text.literal("Health Threshold"), (long)TutorialMod.CONFIG.potionHealthThreshold, 1, 20).setDefaultValue(10).setSaveConsumer(newValue -> TutorialMod.CONFIG.potionHealthThreshold = newValue.doubleValue()).build());
            potionModule.addEntry(entryBuilder.startBooleanToggle(Text.literal("Throw Potion"), TutorialMod.CONFIG.potionThrow).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.potionThrow = newValue).build());
            potionModule.addEntry(entryBuilder.startBooleanToggle(Text.literal("Restore Slot (Health)"), TutorialMod.CONFIG.potionRestoreSlot).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.potionRestoreSlot = newValue).build());
            potionModule.addEntry(entryBuilder.startLongSlider(Text.literal("Strength Threshold (Seconds)"), (long)(TutorialMod.CONFIG.potionStrengthThreshold * 10), 0, 4800).setDefaultValue(300).setSaveConsumer(newValue -> TutorialMod.CONFIG.potionStrengthThreshold = newValue / 10.0).build());
            potionModule.addEntry(entryBuilder.startLongSlider(Text.literal("Speed Threshold (Seconds)"), (long)(TutorialMod.CONFIG.potionSpeedThreshold * 10), 0, 4800).setDefaultValue(300).setSaveConsumer(newValue -> TutorialMod.CONFIG.potionSpeedThreshold = newValue / 10.0).build());
            potionModule.addEntry(entryBuilder.startLongSlider(Text.literal("Fire Res Threshold (Seconds)"), (long)(TutorialMod.CONFIG.potionFireResThreshold * 10), 0, 4800).setDefaultValue(300).setSaveConsumer(newValue -> TutorialMod.CONFIG.potionFireResThreshold = newValue / 10.0).build());

            // 5. Tool Switch
            ConfigCategory toolSwitch = builder.getOrCreateCategory(Text.literal("Tool Switch"));
            toolSwitch.addEntry(entryBuilder.startBooleanToggle(Text.literal("Auto Tool Switch Enabled"), TutorialMod.CONFIG.autoToolSwitchEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchEnabled = newValue).build());
            toolSwitch.addEntry(entryBuilder.startBooleanToggle(Text.literal("Durability Safety Enabled"), TutorialMod.CONFIG.toolDurabilitySafetyEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.toolDurabilitySafetyEnabled = newValue).build());
            toolSwitch.addEntry(entryBuilder.startBooleanToggle(Text.literal("Restore Original Item"), TutorialMod.CONFIG.autoToolSwitchBackEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchBackEnabled = newValue).build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Min Restore Delay"), TutorialMod.CONFIG.autoToolSwitchBackMinDelay, 0, 100).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchBackMinDelay = newValue).build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Max Restore Delay"), TutorialMod.CONFIG.autoToolSwitchBackMaxDelay, 0, 100).setDefaultValue(5).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchBackMaxDelay = newValue).build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Min Mining Duration"), TutorialMod.CONFIG.autoToolSwitchMineMinDelay, 0, 100).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchMineMinDelay = newValue).build());
            toolSwitch.addEntry(entryBuilder.startIntSlider(Text.literal("Max Mining Duration"), TutorialMod.CONFIG.autoToolSwitchMineMaxDelay, 0, 100).setDefaultValue(2).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoToolSwitchMineMaxDelay = newValue).build());

            SubCategoryBuilder miningResetSub = entryBuilder.startSubCategory(Text.literal("Mining Reset"));
            miningResetSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.miningResetEnabled).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetEnabled = newValue).build());
            miningResetSub.add(entryBuilder.startStrField(Text.literal("Hotkey"), TutorialMod.CONFIG.miningResetHotkey).setDefaultValue("key.keyboard.unknown").setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetHotkey = newValue).build());
            miningResetSub.add(entryBuilder.startIntSlider(Text.literal("Chance"), TutorialMod.CONFIG.miningResetChance, 0, 100).setDefaultValue(100).setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetChance = newValue).build());
            miningResetSub.add(entryBuilder.startBooleanToggle(Text.literal("Simulate Mining Stops"), TutorialMod.CONFIG.miningResetSimulateStops).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetSimulateStops = newValue).build());
            miningResetSub.add(entryBuilder.startLongSlider(Text.literal("Early Release Threshold"), (long)(TutorialMod.CONFIG.miningResetThreshold * 100), 50, 99).setDefaultValue(92).setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetThreshold = newValue / 100.0).build());
            miningResetSub.add(entryBuilder.startIntSlider(Text.literal("Mining Reset Delay"), TutorialMod.CONFIG.miningResetDelay, 0, 5).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.miningResetDelay = newValue).build());
            toolSwitch.addEntry(miningResetSub.build());

            // 6. Trigger Bot
            ConfigCategory triggerBot = builder.getOrCreateCategory(Text.literal("Trigger Bot"));
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Trigger Bot Enabled"), TutorialMod.CONFIG.triggerBotEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotEnabled = newValue).build());
            triggerBot.addEntry(entryBuilder.startStrField(Text.literal("Toggle Hotkey"), TutorialMod.CONFIG.triggerBotToggleHotkey).setDefaultValue("key.keyboard.k").setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotToggleHotkey = newValue).build());
            triggerBot.addEntry(entryBuilder.startStrField(Text.literal("Activation Hotkey"), TutorialMod.CONFIG.triggerBotHotkey).setDefaultValue("key.keyboard.0").setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotHotkey = newValue).build());

            SubCategoryBuilder triggerBotFilters = entryBuilder.startSubCategory(Text.literal("Filters"));
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Players"), TutorialMod.CONFIG.triggerBotIncludePlayers).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludePlayers = newValue).build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Exclude Teammates"), TutorialMod.CONFIG.triggerBotExcludeTeammates).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotExcludeTeammates = newValue).build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Hostiles"), TutorialMod.CONFIG.triggerBotIncludeHostiles).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludeHostiles = newValue).build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Passives"), TutorialMod.CONFIG.triggerBotIncludePassives).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludePassives = newValue).build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Exclude Villagers"), TutorialMod.CONFIG.triggerBotExcludeVillagers).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotExcludeVillagers = newValue).build());
            triggerBotFilters.add(entryBuilder.startBooleanToggle(Text.literal("Include Crystals"), TutorialMod.CONFIG.triggerBotIncludeCrystals).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludeCrystals = newValue).build());
            triggerBot.addEntry(triggerBotFilters.build());

            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Trigger Bot Active in Inventory"), TutorialMod.CONFIG.triggerBotActiveInInventory).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotActiveInInventory = newValue).build());
            triggerBot.addEntry(entryBuilder.startLongSlider(Text.literal("Trigger Bot Max Range"), (long)(TutorialMod.CONFIG.triggerBotMaxRange * 10), 0, 45).setDefaultValue(30).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotMaxRange = newValue / 10.0).build());
            triggerBot.addEntry(entryBuilder.startLongSlider(Text.literal("Trigger Bot Min Range"), (long)(TutorialMod.CONFIG.triggerBotMinRange * 10), 0, 45).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotMinRange = newValue / 10.0).build());

            SubCategoryBuilder quickCrossbowSub = entryBuilder.startSubCategory(Text.literal("Quick Crossbow"));
            quickCrossbowSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.quickCrossbowEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.quickCrossbowEnabled = newValue).build());
            quickCrossbowSub.add(entryBuilder.startIntSlider(Text.literal("Reload Threshold (Ticks)"), TutorialMod.CONFIG.quickCrossbowReloadThreshold, 1, 10).setDefaultValue(4).setSaveConsumer(newValue -> TutorialMod.CONFIG.quickCrossbowReloadThreshold = newValue).build());
            triggerBot.addEntry(quickCrossbowSub.build());
            triggerBot.addEntry(entryBuilder.startIntSlider(Text.literal("Min Reaction Delay"), TutorialMod.CONFIG.triggerBotReactionMinDelay, 0, 20).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotReactionMinDelay = newValue).build());
            triggerBot.addEntry(entryBuilder.startIntSlider(Text.literal("Max Reaction Delay"), TutorialMod.CONFIG.triggerBotReactionMaxDelay, 0, 20).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotReactionMaxDelay = newValue).build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Melee Weapons Only"), TutorialMod.CONFIG.triggerBotWeaponOnly).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotWeaponOnly = newValue).build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Attack on Crit Only"), TutorialMod.CONFIG.attackOnCrit).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.attackOnCrit = newValue).build());

            // 7. UHC/Cart (Minecart Tech)
            ConfigCategory minecartTech = builder.getOrCreateCategory(Text.literal("UHC/Cart"));
            minecartTech.addEntry(entryBuilder.startBooleanToggle(Text.literal("TNT Minecart Placement Enabled"), TutorialMod.CONFIG.tntMinecartPlacementEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.tntMinecartPlacementEnabled = newValue).build());
            minecartTech.addEntry(entryBuilder.startBooleanToggle(Text.literal("Lava/Crossbow Sequence Enabled"), TutorialMod.CONFIG.lavaCrossbowSequenceEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.lavaCrossbowSequenceEnabled = newValue).build());
            minecartTech.addEntry(entryBuilder.startBooleanToggle(Text.literal("Bow Sequence Enabled"), TutorialMod.CONFIG.bowSequenceEnabled).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.bowSequenceEnabled = newValue).build());
            minecartTech.addEntry(entryBuilder.startIntSlider(Text.literal("Bow Sequence Cooldown"), TutorialMod.CONFIG.bowCooldown, 0, 200).setDefaultValue(100).setSaveConsumer(newValue -> TutorialMod.CONFIG.bowCooldown = newValue).build());

            SubCategoryBuilder waterDrainSub = entryBuilder.startSubCategory(Text.literal("Water Drain"));
            waterDrainSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.waterDrainEnabled).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.waterDrainEnabled = newValue).build());
            waterDrainSub.add(entryBuilder.startBooleanToggle(Text.literal("Enable Lava Drain"), TutorialMod.CONFIG.waterDrainLavaEnabled).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.waterDrainLavaEnabled = newValue).build());
            waterDrainSub.add(entryBuilder.startBooleanToggle(Text.literal("Auto Mode"), TutorialMod.CONFIG.autoWaterDrainMode).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoWaterDrainMode = newValue).build());
            waterDrainSub.add(entryBuilder.startStrField(Text.literal("Auto Mode Hotkey"), TutorialMod.CONFIG.autoWaterDrainHotkey).setDefaultValue("key.keyboard.n").setSaveConsumer(newValue -> TutorialMod.CONFIG.autoWaterDrainHotkey = newValue).build());
            waterDrainSub.add(entryBuilder.startIntSlider(Text.literal("Switch To Delay"), TutorialMod.CONFIG.waterDrainSwitchToDelay, 0, 20).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.waterDrainSwitchToDelay = newValue).build());
            waterDrainSub.add(entryBuilder.startIntSlider(Text.literal("Switch Back Delay"), TutorialMod.CONFIG.waterDrainSwitchBackDelay, 0, 20).setDefaultValue(0).setSaveConsumer(newValue -> TutorialMod.CONFIG.waterDrainSwitchBackDelay = newValue).build());
            minecartTech.addEntry(waterDrainSub.build());

            // 8. Chat
            ConfigCategory chat = builder.getOrCreateCategory(Text.literal("Chat"));
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Disable Mod Chat Updates"), TutorialMod.CONFIG.disableModChatUpdates).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.disableModChatUpdates = newValue).build());
            chat.addEntry(entryBuilder.startStrField(Text.literal("Coordinate Trigger"), TutorialMod.CONFIG.trigger).setDefaultValue("cc").setSaveConsumer(newValue -> TutorialMod.CONFIG.trigger = newValue).build());
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Case Sensitive Trigger"), TutorialMod.CONFIG.caseSensitive).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.caseSensitive = newValue).build());
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Replace in Chat"), TutorialMod.CONFIG.replaceInChat).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.replaceInChat = newValue).build());
            chat.addEntry(entryBuilder.startBooleanToggle(Text.literal("Replace in Commands"), TutorialMod.CONFIG.replaceInCommands).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.replaceInCommands = newValue).build());
            chat.addEntry(entryBuilder.startStrField(Text.literal("Coordinate Format"), TutorialMod.CONFIG.format).setDefaultValue("{bx} {by} {bz} {dim} {facing}").setSaveConsumer(newValue -> TutorialMod.CONFIG.format = newValue).build());

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
                SubCategoryBuilder macroCategory = entryBuilder.startSubCategory(Text.literal("Macro " + macroNum));
                macroCategory.add(entryBuilder.startStrField(Text.literal("Name"), macro.name).setSaveConsumer(newValue -> macro.name = newValue).build());
                macroCategory.add(entryBuilder.startStrField(Text.literal("Hotkey"), macro.hotkey).setSaveConsumer(newValue -> macro.hotkey = newValue).build());
                macroCategory.add(entryBuilder.startStrField(Text.literal("Message/Command"), macro.message).setSaveConsumer(newValue -> macro.message = newValue).build());
                chat.addEntry(macroCategory.build());
            }

            // 9. Misc
            ConfigCategory misc = builder.getOrCreateCategory(Text.literal("Misc"));
            misc.addEntry(entryBuilder.startStrField(Text.literal("Open Settings Hotkey"), TutorialMod.CONFIG.openSettingsHotkey).setDefaultValue("key.keyboard.right.shift").setSaveConsumer(newValue -> TutorialMod.CONFIG.openSettingsHotkey = newValue).build());
            misc.addEntry(entryBuilder.startStrField(Text.literal("Master Toggle Hotkey"), TutorialMod.CONFIG.masterToggleHotkey).setDefaultValue("key.keyboard.m").setSaveConsumer(newValue -> TutorialMod.CONFIG.masterToggleHotkey = newValue).build());
            misc.addEntry(entryBuilder.startStrField(Text.literal("Teammate Toggle Hotkey"), TutorialMod.CONFIG.teammateHotkey).setDefaultValue("key.keyboard.g").setSaveConsumer(newValue -> TutorialMod.CONFIG.teammateHotkey = newValue).build());
            misc.addEntry(entryBuilder.startStrField(Text.literal("Sprint Mode Toggle Hotkey"), TutorialMod.CONFIG.sprintModeHotkey).setDefaultValue("key.keyboard.n").setSaveConsumer(newValue -> TutorialMod.CONFIG.sprintModeHotkey = newValue).build());
            misc.addEntry(entryBuilder.startStrField(Text.literal("Sneak Mode Toggle Hotkey"), TutorialMod.CONFIG.sneakModeHotkey).setDefaultValue("key.keyboard.b").setSaveConsumer(newValue -> TutorialMod.CONFIG.sneakModeHotkey = newValue).build());
            misc.addEntry(entryBuilder.startBooleanToggle(Text.literal("Hotkeys Active in Inventory"), TutorialMod.CONFIG.activeInInventory).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.activeInInventory = newValue).build());

            misc.addEntry(entryBuilder.startBooleanToggle(Text.literal("Click Spam Enabled"), TutorialMod.CONFIG.clickSpamEnabled).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.clickSpamEnabled = newValue).build());
            misc.addEntry(entryBuilder.startIntSlider(Text.literal("Click Spam Rate"), TutorialMod.CONFIG.clickSpamCps, 1, 20).setDefaultValue(12).setSaveConsumer(newValue -> TutorialMod.CONFIG.clickSpamCps = newValue).build());
            misc.addEntry(entryBuilder.startStrField(Text.literal("Click Spam Modifier Hotkey"), TutorialMod.CONFIG.clickSpamModifierKey).setDefaultValue("key.keyboard.apostrophe").setSaveConsumer(newValue -> TutorialMod.CONFIG.clickSpamModifierKey = newValue).build());

            SubCategoryBuilder extinguishSub = entryBuilder.startSubCategory(Text.literal("Auto Extinguish"));
            extinguishSub.add(entryBuilder.startBooleanToggle(Text.literal("Enabled"), TutorialMod.CONFIG.autoExtinguishEnabled).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoExtinguishEnabled = newValue).build());
            extinguishSub.add(entryBuilder.startLongSlider(Text.literal("Activation Pitch"), (long)TutorialMod.CONFIG.autoExtinguishPitch, 0, 90).setDefaultValue(60).setSaveConsumer(newValue -> TutorialMod.CONFIG.autoExtinguishPitch = newValue.doubleValue()).build());
            misc.addEntry(extinguishSub.build());

            // 10. Overlay
            ConfigCategory overlay = builder.getOrCreateCategory(Text.literal("Overlay"));
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Coords Overlay Enabled"), TutorialMod.CONFIG.showCoordsOverlay).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showCoordsOverlay = newValue).build());
            overlay.addEntry(entryBuilder.startStrField(Text.literal("Toggle Hotkey"), TutorialMod.CONFIG.toggleOverlayHotkey).setDefaultValue("key.keyboard.h").setSaveConsumer(newValue -> TutorialMod.CONFIG.toggleOverlayHotkey = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Accurate Coordinates"), TutorialMod.CONFIG.showAccurateCoordinates).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showAccurateCoordinates = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Nether Coordinates"), TutorialMod.CONFIG.showNetherCoords).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showNetherCoords = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Entity Count"), TutorialMod.CONFIG.showEntityCount).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showEntityCount = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Chunk Count"), TutorialMod.CONFIG.showChunkCount).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showChunkCount = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Long Distance Coordinates"), TutorialMod.CONFIG.showLongCoords).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showLongCoords = newValue).build());
            overlay.addEntry(entryBuilder.startIntSlider(Text.literal("Long Distance Range"), TutorialMod.CONFIG.longCoordsMaxDistance, 64, 1024).setDefaultValue(512).setSaveConsumer(newValue -> TutorialMod.CONFIG.longCoordsMaxDistance = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Pointing Distance"), TutorialMod.CONFIG.showLongCoordsDistance).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.showLongCoordsDistance = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Detailed Cardinals"), TutorialMod.CONFIG.showDetailedCardinals).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showDetailedCardinals = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Sprint Status"), TutorialMod.CONFIG.showSprintModeOverlay).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.showSprintModeOverlay = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Sneak Status"), TutorialMod.CONFIG.showSneakModeOverlay).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.showSneakModeOverlay = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Latest Toggle on Overlay"), TutorialMod.CONFIG.showLatestToggleOverlay).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showLatestToggleOverlay = newValue).build());
            overlay.addEntry(entryBuilder.startIntSlider(Text.literal("Overlay Font Size"), TutorialMod.CONFIG.overlayFontSize, 8, 40).setDefaultValue(20).setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayFontSize = newValue).build());
            overlay.addEntry(entryBuilder.startIntSlider(Text.literal("Overlay Background Opacity"), TutorialMod.CONFIG.overlayBackgroundOpacity, 0, 255).setDefaultValue(128).setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayBackgroundOpacity = newValue).build());
            overlay.addEntry(entryBuilder.startStrField(Text.literal("Overlay Horizontal Alignment"), TutorialMod.CONFIG.overlayAlignment).setDefaultValue("Left").setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayAlignment = newValue).build());
            overlay.addEntry(entryBuilder.startStrField(Text.literal("Overlay Vertical Alignment"), TutorialMod.CONFIG.overlayVAlignment).setDefaultValue("Top").setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayVAlignment = newValue).build());
            overlay.addEntry(entryBuilder.startStrField(Text.literal("Overlay Font Name"), TutorialMod.CONFIG.overlayFontName).setDefaultValue("Consolas").setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayFontName = newValue).build());
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Lock Overlay"), TutorialMod.CONFIG.overlayLocked).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.overlayLocked = newValue).build());

            SubCategoryBuilder enemyInfoSubCategory = entryBuilder.startSubCategory(Text.literal("Enemy Info"));
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Enemy Info Enabled"), TutorialMod.CONFIG.showEnemyInfo).setDefaultValue(true).setSaveConsumer(newValue -> TutorialMod.CONFIG.showEnemyInfo = newValue).build());
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Show HP Decimals"), TutorialMod.CONFIG.showHpDecimals).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showHpDecimals = newValue).build());
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Show Weakest Armor Piece"), TutorialMod.CONFIG.showLowestArmorPiece).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showLowestArmorPiece = newValue).build());
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Show Blast Protection Count"), TutorialMod.CONFIG.showBlastProtectionCount).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.showBlastProtectionCount = newValue).build());
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Extended Detection Range"), TutorialMod.CONFIG.doubleEnemyInfoRange).setDefaultValue(false).setSaveConsumer(newValue -> TutorialMod.CONFIG.doubleEnemyInfoRange = newValue).build());
            overlay.addEntry(enemyInfoSubCategory.build());

            return builder.build();
        };
    }
}
