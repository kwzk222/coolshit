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
                    .setTitle(Text.literal("Attribute Swapping"))
                    .setDefaultBackgroundTexture(Identifier.ofVanilla("textures/gui/options_background.png"));

            builder.setSavingRunnable(TutorialMod.CONFIG::save);

            ConfigEntryBuilder entryBuilder = builder.entryBuilder();

            // AutoStun Category
            ConfigCategory autoStun = builder.getOrCreateCategory(Text.literal("AutoStun"));
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

            // Overlay Category
            ConfigCategory overlay = builder.getOrCreateCategory(Text.literal("Overlay"));
            overlay.addEntry(entryBuilder.startBooleanToggle(Text.literal("Show Coords Overlay"), TutorialMod.CONFIG.showCoordsOverlay)
                    .setDefaultValue(false)
                    .setTooltip(Text.literal("Shows a separate window with your coordinates."))
                    .setSaveConsumer(newValue -> {
                        TutorialMod.CONFIG.showCoordsOverlay = newValue;
                        if (newValue) {
                            TutorialModClient.getOverlayManager().start();
                        } else if (!TutorialMod.CONFIG.showEnemyInfo) {
                            TutorialModClient.getOverlayManager().stop();
                        }
                    })
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

            // Enemy Info SubCategory
            me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder enemyInfoSubCategory = entryBuilder.startSubCategory(Text.literal("Enemy Info"));
            enemyInfoSubCategory.add(entryBuilder.startBooleanToggle(Text.literal("Show Enemy Info"), TutorialMod.CONFIG.showEnemyInfo)
                    .setDefaultValue(true)
                    .setTooltip(Text.literal("Shows information about the player you are looking at."))
                    .setSaveConsumer(newValue -> {
                        TutorialMod.CONFIG.showEnemyInfo = newValue;
                        if (newValue) {
                            TutorialModClient.getOverlayManager().start();
                        } else if (!TutorialMod.CONFIG.showCoordsOverlay) {
                            TutorialModClient.getOverlayManager().stop();
                        }
                    })
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
