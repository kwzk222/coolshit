package net.rev.tutorialmod.mixin.accessor;

import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InventoryScreen.class)
public interface InventoryScreenAccessor {
    @Accessor("recipeBookButton")
    ButtonWidget getRecipeBookButton();
}
