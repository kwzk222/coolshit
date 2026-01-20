package net.rev.tutorialmod.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.rev.tutorialmod.TutorialMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "init", at = @At("RETURN"))
    private void tutorialmod$onInit(CallbackInfo ci) {
        if (this.getClass().getName().equals("me.shedaniel.clothconfig2.gui.AbstractConfigScreen")) {
            try {
                // Try to restore by category name
                String lastCat = TutorialMod.CONFIG.lastCategory;
                if (lastCat == null || lastCat.isEmpty()) return;

                Field categoriesField = this.getClass().getDeclaredField("categories");
                categoriesField.setAccessible(true);
                List<?> categories = (List<?>) categoriesField.get(this);

                if (categories != null) {
                    for (int i = 0; i < categories.size(); i++) {
                        Object category = categories.get(i);
                        Method getNameMethod = category.getClass().getMethod("getCategoryName");
                        Object nameObj = getNameMethod.invoke(category);
                        String name = "";
                        if (nameObj instanceof String s) name = s;
                        else if (nameObj != null) {
                            try {
                                Method getString = nameObj.getClass().getMethod("getString");
                                name = (String) getString.invoke(nameObj);
                            } catch (Exception e) {
                                name = nameObj.toString();
                            }
                        }
                        if (lastCat.equals(name)) {
                            Field selectedIndexField = this.getClass().getDeclaredField("selectedCategoryIndex");
                            selectedIndexField.setAccessible(true);
                            selectedIndexField.set(this, i);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore failures to avoid crashing
            }
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void tutorialmod$onRemoved(CallbackInfo ci) {
        if (this.getClass().getName().equals("me.shedaniel.clothconfig2.gui.AbstractConfigScreen")) {
            try {
                Field selectedIndexField = this.getClass().getDeclaredField("selectedCategoryIndex");
                selectedIndexField.setAccessible(true);
                int index = (int) selectedIndexField.get(this);

                Field categoriesField = this.getClass().getDeclaredField("categories");
                categoriesField.setAccessible(true);
                List<?> categories = (List<?>) categoriesField.get(this);

                if (categories != null && index >= 0 && index < categories.size()) {
                    Object category = categories.get(index);
                    Method getNameMethod = category.getClass().getMethod("getCategoryName");
                    Object nameObj = getNameMethod.invoke(category);
                    String name = "";
                    if (nameObj instanceof String s) name = s;
                    else if (nameObj != null) {
                        try {
                            Method getString = nameObj.getClass().getMethod("getString");
                            name = (String) getString.invoke(nameObj);
                        } catch (Exception e) {
                            name = nameObj.toString();
                        }
                    }
                    if (!name.isEmpty()) {
                        TutorialMod.CONFIG.lastCategory = name;
                        TutorialMod.CONFIG.save();
                    }
                }
            } catch (Exception e) {
                // Ignore failures
            }
        }
    }
}
