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
            ModConfig config = ModConfig.load();

            ConfigBuilder builder = ConfigBuilder.create()
                    .setParentScreen(parent)
                    .setTitle(Text.literal("Tutorial Mod Config"));

            builder.setSavingRunnable(config::save);

            ConfigCategory general = builder.getOrCreateCategory(Text.literal("General"));

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            general.addEntry(entryBuilder.startIntSlider(Text.literal("Axe Swap Delay"), config.axeSwapDelay, 0, 20)
                    .setDefaultValue(5)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the axe."))
                    .setSaveConsumer(newValue -> config.axeSwapDelay = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntSlider(Text.literal("Mace Swap Delay"), config.maceSwapDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the mace."))
                    .setSaveConsumer(newValue -> config.maceSwapDelay = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntSlider(Text.literal("Combo Swap Delay"), config.comboSwapDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping to the mace in a combo."))
                    .setSaveConsumer(newValue -> config.comboSwapDelay = newValue)
                    .build());

            general.addEntry(entryBuilder.startIntSlider(Text.literal("Post-Combo Axe Swap Delay"), config.postComboAxeSwapDelay, 0, 20)
                    .setDefaultValue(2)
                    .setTooltip(Text.literal("The delay in ticks before swapping back to the original item after a mace combo."))
                    .setSaveConsumer(newValue -> config.postComboAxeSwapDelay = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Totem Swap Enabled"), config.totemSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic totem swapping feature."))
                    .setSaveConsumer(newValue -> config.totemSwapEnabled = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Axe Swap Enabled"), config.axeSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic axe swapping feature."))
                    .setSaveConsumer(newValue -> config.axeSwapEnabled = newValue)
                    .build());

            general.addEntry(entryBuilder.startBooleanToggle(Text.literal("Mace Swap Enabled"), config.maceSwapEnabled)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Enable or disable the automatic mace swapping feature."))
                    .setSaveConsumer(newValue -> config.maceSwapEnabled = newValue)
                    .build());

            return builder.build();
        };
    }
}
