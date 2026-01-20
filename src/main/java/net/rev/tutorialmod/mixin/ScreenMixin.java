package net.rev.tutorialmod.mixin;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.rev.tutorialmod.TutorialMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "removed", at = @At("HEAD"))
    private void tutorialmod$onRemoved(CallbackInfo ci) {
        if ((Object)this instanceof AbstractConfigScreen configScreen) {
            Map<Text, List<AbstractConfigEntry<?>>> categorizedEntries = configScreen.getCategorizedEntries();
            if (categorizedEntries != null && !categorizedEntries.isEmpty()) {
                List<Text> categories = new ArrayList<>(categorizedEntries.keySet());
                int index = configScreen.selectedCategoryIndex;
                if (index >= 0 && index < categories.size()) {
                    TutorialMod.CONFIG.lastCategory = categories.get(index).getString();
                    TutorialMod.CONFIG.save();
                }
            }
        }
    }
}
