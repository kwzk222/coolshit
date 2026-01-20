package net.rev.tutorialmod.mixin;

import me.shedaniel.clothconfig2.api.AbstractConfigEntry;
import me.shedaniel.clothconfig2.gui.AbstractConfigScreen;
import net.minecraft.text.Text;
import net.rev.tutorialmod.TutorialMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Mixin(value = AbstractConfigScreen.class, remap = false)
public abstract class AbstractConfigScreenMixin {

    @Shadow public int selectedCategoryIndex;
    @Shadow public abstract Map<Text, List<AbstractConfigEntry<?>>> getCategorizedEntries();

    @Inject(method = "tick", at = @At("HEAD"))
    private void tutorialmod$onTick(CallbackInfo ci) {
        Map<Text, List<AbstractConfigEntry<?>>> categorizedEntries = getCategorizedEntries();
        if (categorizedEntries != null && !categorizedEntries.isEmpty()) {
            List<Text> categories = new ArrayList<>(categorizedEntries.keySet());
            if (selectedCategoryIndex >= 0 && selectedCategoryIndex < categories.size()) {
                String currentCategory = categories.get(selectedCategoryIndex).getString();
                if (!currentCategory.equals(TutorialMod.CONFIG.lastCategory)) {
                    TutorialMod.CONFIG.lastCategory = currentCategory;
                    TutorialMod.CONFIG.save();
                }
            }
        }
    }
}
