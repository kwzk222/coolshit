package net.rev.tutorialmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Tutorial Mod Config"))
                    .setDefaultBackgroundTexture(Identifier.ofVanilla("textures/gui/options_background.png"));

            builder.setSavingRunnable(TutorialMod.CONFIG::save);

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // General Category
            ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));
            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Master Switch"), TutorialMod.CONFIG.masterEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("The main switch to enable or disable all features of the mod at once."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.masterEnabled = newValue)
                    .build());
            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Totem Swap Enabled"), TutorialMod.CONFIG.totemSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic totem swapping feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.totemSwapEnabled = newValue)
                    .build());

            // Attribute Swapping Category
            ConfigCategory attributeSwapping = builder.getOrCreateCategory(Text.literal("Attribute Swapping"));
            attributeSwapping.addEntry(entryBuilder.startBooleanToggle(Text.literal("Axe Swap Enabled"), TutorialMod.CONFIG.axeSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic axe swapping feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapEnabled = newValue)
                    .build());
            attributeSwapping.addEntry(entryBuilder.startIntSlider(Text.literal("Axe Swap Delay"), TutorialMod.CONFIG.axeSwapDelay, 0, 20)
                    .setDefaultValue(5)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the axe."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapDelay = newValue)
                    .build());
            attributeSwapping.addEntry(entryBuilder.startBooleanToggle(Text.literal("Mace Swap Enabled"), TutorialMod.CONFIG.maceSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic mace swapping feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapEnabled = newValue)
                    .build());
            attributeSwapping.addEntry(entryBuilder.startIntSlider(Text.literal("Mace Swap Delay"), TutorialMod.CONFIG.maceSwapDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the mace."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapDelay = newValue)
                    .build());
            attributeSwapping.addEntry(entryBuilder.startIntSlider(Text.literal("Combo Swap Delay"), TutorialMod.CONFIG.comboSwapDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping to the mace in a combo."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.comboSwapDelay = newValue)
                    .build());
            attributeSwapping.addEntry(entryBuilder.startIntSlider(Text.literal("Post-Combo Axe Swap Delay"), TutorialMod.CONFIG.postComboAxeSwapDelay, 0, 20)
                    .setDefaultValue(2)
                    .setTooltip(Text.literal("The delay in ticks before swapping back to the original item after a mace combo."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.postComboAxeSwapDelay = newValue)
                    .build());
            attributeSwapping.addEntry(entryBuilder.startIntSlider(Text.literal("Minimum Fall Distance for Combo"), TutorialMod.CONFIG.minFallDistance, 1, 5)
                    .setDefaultValue(3)
                    .setTooltip(Text.literal("The minimum fall distance required to trigger the Axe-Mace combo."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.minFallDistance = newValue)
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

            // TriggerBot Category
            ConfigCategory triggerBot = builder.getOrCreateCategory(Text.literal("TriggerBot"));
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Enable TriggerBot"), TutorialMod.CONFIG.triggerBotEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the TriggerBot feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotEnabled = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Include Players"), TutorialMod.CONFIG.triggerBotIncludePlayers)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Attack players."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludePlayers = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Exclude Teammates"), TutorialMod.CONFIG.triggerBotExcludeTeammates)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Avoid attacking teammates."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotExcludeTeammates = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Include Hostiles"), TutorialMod.CONFIG.triggerBotIncludeHostiles)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Attack hostile mobs."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludeHostiles = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Include Passives"), TutorialMod.CONFIG.triggerBotIncludePassives)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Attack passive mobs."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludePassives = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Exclude Villagers"), TutorialMod.CONFIG.triggerBotExcludeVillagers)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Avoid attacking villagers even if passives are included."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotExcludeVillagers = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Include Crystals"), TutorialMod.CONFIG.triggerBotIncludeCrystals)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Attack end crystals."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotIncludeCrystals = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startBooleanToggle(Text.literal("Active in Inventory"), TutorialMod.CONFIG.triggerBotActiveInInventory)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Whether the TriggerBot should be active while you are in an inventory screen."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotActiveInInventory = newValue)
                    .build());
            triggerBot.addEntry(entryBuilder.startLongSlider(Text.literal("Max Range (x100)"), (long) (TutorialMod.CONFIG.triggerBotMaxRange * 100), 100L, 700L)
                    .setDefaultValue((long) (4.5 * 100))
                    .setTooltip(Text.literal("The maximum distance at which the TriggerBot will activate."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotMaxRange = newValue / 100.0)
                    .build());
            triggerBot.addEntry(entryBuilder.startIntSlider(Text.literal("Max Attack Delay (ticks)"), TutorialMod.CONFIG.triggerBotAttackDelay, 0, 20)
                    .setDefaultValue(0)
                    .setTooltip(Text.literal("Adds a random delay (from 0 to this value) in ticks before attacking."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotAttackDelay = newValue)
                    .build());

            // Human Movement Category
            ConfigCategory humanMovement = builder.getOrCreateCategory(Text.literal("Human Movement"));
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Base Move Time (ms)"), TutorialMod.CONFIG.movementParams.baseMs)
                    .setDefaultValue(120f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.baseMs = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Time per Unit (ms)"), TutorialMod.CONFIG.movementParams.kPerUnit)
                    .setDefaultValue(6f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.kPerUnit = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Min Move Time (ms)"), TutorialMod.CONFIG.movementParams.minMs)
                    .setDefaultValue(60f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.minMs = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Max Move Time (ms)"), TutorialMod.CONFIG.movementParams.maxMs)
                    .setDefaultValue(600f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.maxMs = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Min Reaction Delay (ms)"), TutorialMod.CONFIG.movementParams.reactMinMs)
                    .setDefaultValue(100f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.reactMinMs = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Max Reaction Delay (ms)"), TutorialMod.CONFIG.movementParams.reactMaxMs)
                    .setDefaultValue(250f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.reactMaxMs = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Correction Threshold (deg/px)"), TutorialMod.CONFIG.movementParams.correctionThreshold)
                    .setDefaultValue(0.3f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.correctionThreshold = newValue).build());
            humanMovement.addEntry(entryBuilder.startIntField(Text.literal("Max Corrections"), TutorialMod.CONFIG.movementParams.maxCorrections)
                    .setDefaultValue(3).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.maxCorrections = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Correction Duration Scale"), TutorialMod.CONFIG.movementParams.correctionDurScale)
                    .setDefaultValue(0.25f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.correctionDurScale = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Speed Noise Scale"), TutorialMod.CONFIG.movementParams.alphaNoise)
                    .setDefaultValue(0.003f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.alphaNoise = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Noise Jitter Factor"), TutorialMod.CONFIG.movementParams.noiseJitterFactor)
                    .setDefaultValue(0.3f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.noiseJitterFactor = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Tremor Amplitude"), TutorialMod.CONFIG.movementParams.tremorAmp)
                    .setDefaultValue(0.03f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.tremorAmp = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Tremor Frequency 1"), TutorialMod.CONFIG.movementParams.tremorF1)
                    .setDefaultValue(9f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.tremorF1 = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Tremor Frequency 2"), TutorialMod.CONFIG.movementParams.tremorF2)
                    .setDefaultValue(22f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.tremorF2 = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Drift Amplitude"), TutorialMod.CONFIG.movementParams.driftAmp)
                    .setDefaultValue(0.02f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.driftAmp = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Drift Frequency (Hz)"), TutorialMod.CONFIG.movementParams.driftHz)
                    .setDefaultValue(1.2f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.driftHz = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Via-Point Chance"), TutorialMod.CONFIG.movementParams.viaPointChance)
                    .setDefaultValue(0.35f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.viaPointChance = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Min Via-Point Offset"), TutorialMod.CONFIG.movementParams.viaOffsetMin)
                    .setDefaultValue(0.05f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.viaOffsetMin = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Max Via-Point Offset"), TutorialMod.CONFIG.movementParams.viaOffsetMax)
                    .setDefaultValue(0.25f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.viaOffsetMax = newValue).build());
            humanMovement.addEntry(entryBuilder.startIntField(Text.literal("Sample Rate (Hz)"), TutorialMod.CONFIG.movementParams.sampleHz)
                    .setDefaultValue(120).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.sampleHz = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Micro-Lag Blending"), TutorialMod.CONFIG.movementParams.blend)
                    .setDefaultValue(0.9f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.blend = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Min Pitch"), TutorialMod.CONFIG.movementParams.pitchMin)
                    .setDefaultValue(-90f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.pitchMin = newValue).build());
            humanMovement.addEntry(entryBuilder.startFloatField(Text.literal("Max Pitch"), TutorialMod.CONFIG.movementParams.pitchMax)
                    .setDefaultValue(90f).setSaveConsumer(newValue -> TutorialMod.CONFIG.movementParams.pitchMax = newValue).build());


            return builder.build();
        };
    }
}
