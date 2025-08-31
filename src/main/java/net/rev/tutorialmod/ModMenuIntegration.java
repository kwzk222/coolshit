package net.rev.tutorialmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class ModMenuIntegration implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Tutorial Mod Config"));

            builder.setSavingRunnable(TutorialMod.CONFIG::save);

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Master Switch"), TutorialMod.CONFIG.masterEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("The main switch to enable or disable all features of the mod at once."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.masterEnabled = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntSlider(Text.literal("Axe Swap Delay"), TutorialMod.CONFIG.axeSwapDelay, 0, 20)
                    .setDefaultValue(5)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the axe."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapDelay = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntSlider(Text.literal("Mace Swap Delay"), TutorialMod.CONFIG.maceSwapDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the mace."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapDelay = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntSlider(Text.literal("Combo Swap Delay"), TutorialMod.CONFIG.comboSwapDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping to the mace in a combo."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.comboSwapDelay = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntSlider(Text.literal("Post-Combo Axe Swap Delay"), TutorialMod.CONFIG.postComboAxeSwapDelay, 0, 20)
                    .setDefaultValue(2)
                    .setTooltip(Text.literal("The delay in ticks before swapping back to the original item after a mace combo."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.postComboAxeSwapDelay = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntSlider(Text.literal("Minimum Fall Distance for Combo"), TutorialMod.CONFIG.minFallDistance, 1, 5)
                    .setDefaultValue(3)
                    .setTooltip(Text.literal("The minimum fall distance required to trigger the Axe-Mace combo."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.minFallDistance = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Totem Swap Enabled"), TutorialMod.CONFIG.totemSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic totem swapping feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.totemSwapEnabled = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Axe Swap Enabled"), TutorialMod.CONFIG.axeSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic axe swapping feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeSwapEnabled = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Mace Swap Enabled"), TutorialMod.CONFIG.maceSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic mace swapping feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceSwapEnabled = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("TNT Minecart Placement Enabled"), TutorialMod.CONFIG.tntMinecartPlacementEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic TNT minecart placement feature."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.tntMinecartPlacementEnabled = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Lava/Crossbow Sequence Enabled"), TutorialMod.CONFIG.lavaCrossbowSequenceEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the Lava/Crossbow sequence after placing a minecart."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.lavaCrossbowSequenceEnabled = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Bow Sequence Enabled"), TutorialMod.CONFIG.bowSequenceEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the Bow sequence after placing a minecart."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.bowSequenceEnabled = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntSlider(Text.literal("Lava Pickup Delay"), TutorialMod.CONFIG.lavaPickupDelay, 0, 20)
                    .setDefaultValue(5)
                    .setTooltip(Text.literal("The delay in ticks after shooting the crossbow before picking up the lava."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.lavaPickupDelay = newValue)
                    .build());

            return builder.build();
        };
    }
}
