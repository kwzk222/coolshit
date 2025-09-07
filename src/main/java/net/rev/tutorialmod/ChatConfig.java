package net.rev.tutorialmod;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "tutorialmod_chat")
public class ChatConfig implements ConfigData {
    // --- Coordinate Sending ---
    @ConfigEntry.Gui.Tooltip
    public String trigger = "cc";

    @ConfigEntry.Gui.Tooltip
    public boolean caseSensitive = false;

    @ConfigEntry.Gui.Tooltip
    public boolean replaceInChat = true;

    @ConfigEntry.Gui.Tooltip
    public boolean replaceInCommands = true;

    @ConfigEntry.Gui.Tooltip
    public boolean includeDimension = true;

    @ConfigEntry.Gui.Tooltip
    public boolean includeFacing = true;

    @ConfigEntry.Gui.Tooltip
    public boolean useBlockCoords = true;

    @ConfigEntry.Gui.Tooltip
    public String format = "{bx} {by} {bz}{dim}{facing}";

    // --- Chat Macros ---
    @ConfigEntry.Gui.CollapsibleObject
    public Macro macro1 = new Macro();
    @ConfigEntry.Gui.CollapsibleObject
    public Macro macro2 = new Macro();
    @ConfigEntry.Gui.CollapsibleObject
    public Macro macro3 = new Macro();
    @ConfigEntry.Gui.CollapsibleObject
    public Macro macro4 = new Macro();
    @ConfigEntry.Gui.CollapsibleObject
    public Macro macro5 = new Macro();

    public static class Macro {
        public String name = "New Macro";
        public String hotkey = "key.keyboard.unknown";
        public String message = "";
    }
}
