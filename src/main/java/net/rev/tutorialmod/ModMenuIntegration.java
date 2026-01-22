package net.rev.tutorialmod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.rev.tutorialmod.mixin.ScreenAccessor;

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
            hotkeys.addEntry(entryBuilder.startStrField(Text.literal("Overlay Toggle Hotkey"), TutorialMod.CONFIG.toggleOverlayHotkey)
                    .setDefaultValue("key.keyboard.h")
                    .setTooltip(Text.literal("The translation key for the hotkey used to toggle the overlay on or off."))
                    .setSaveConsumer(newValue -> TutorialMod.CONFIG.toggleOverlayHotkey = newValue)
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

            return builder.build();
        };
    }
}
