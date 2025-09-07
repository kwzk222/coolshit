package net.rev.tutorialmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
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
                    .setTitle(Text.literal("Tutorial Mod Config"))
                    .setDefaultBackgroundTexture(Identifier.ofVanilla("textures/gui/options_background.png"));

            builder.setSavingRunnable(TutorialMod.CONFIG::save);

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

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
            attributeSwapping.addEntry(entryBuilder.startIntSlider(Text.literal("Axe to Original Delay"), TutorialMod.CONFIG.axeToOriginalDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the axe to the original item in a combo."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.axeToOriginalDelay = newValue)
                    .build());
            attributeSwapping.addEntry(entryBuilder.startIntSlider(Text.literal("Mace to Original Delay"), TutorialMod.CONFIG.maceToOriginalDelay, 0, 20)
                    .setDefaultValue(1)
                    .setTooltip(Text.literal("The delay in ticks before swapping back from the mace to the original item in a combo."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.maceToOriginalDelay = newValue)
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

            // Hotkeys Category
            ConfigCategory hotkeys = builder.getOrCreateCategory(Text.literal("Hotkeys"));

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

            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("TriggerBot Toggle Hotkey"), TutorialMod.CONFIG.triggerBotToggleHotkey)
                    .setDefaultValue("key.keyboard.k")
                    .setTooltip(Text.literal("The hotkey to toggle the TriggerBot on or off. Use a translation key, e.g., 'key.keyboard.k'."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerBotToggleHotkey = newValue)
                    .build());

            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Toggle Sneak Hotkey"), TutorialMod.CONFIG.toggleSneakHotkey)
                    .setDefaultValue("key.keyboard.c")
                    .setTooltip(Text.literal("The hotkey to toggle sneaking on or off."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.toggleSneakHotkey = newValue)
                    .build());

            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Toggle Sprint Hotkey"), TutorialMod.CONFIG.toggleSprintHotkey)
                    .setDefaultValue("key.keyboard.v")
                    .setTooltip(Text.literal("The hotkey to toggle sprinting on or off."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.toggleSprintHotkey = newValue)
                    .build());
            hotkeys.addEntry(entryBuilder.startBooleanToggle(Text.literal("Active in Inventory"), TutorialMod.CONFIG.activeInInventory)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Whether the hotkeys should be active while you are in an inventory screen."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.activeInInventory = newValue)
                    .build());

            // Chat Category
            ConfigCategory chat = builder.getOrCreateCategory(Text.literal("Chat"));
            chat.addEntry(entryBuilder.startStrField(Text.literal("Trigger Word"), TutorialMod.CONFIG.triggerWord)
                    .setDefaultValue("cc")
                    .setTooltip(Text.literal("The word that will be replaced by your coordinates in chat."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.triggerWord = newValue)
                    .build());
            chat.addEntry(entryBuilder.startStrField(Text.literal("Coordinate Format"), TutorialMod.CONFIG.coordinateFormat)
                    .setDefaultValue("X: {x}, Y: {y}, Z: {z}")
                    .setTooltip(Text.literal("The format of the coordinate message. Use {x}, {y}, and {z} as placeholders."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.coordinateFormat = newValue)
                    .build());

            return builder.build();
        };
    }
}
