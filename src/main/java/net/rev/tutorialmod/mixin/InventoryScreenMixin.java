package net.rev.tutorialmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.world.GameMode;
import net.rev.tutorialmod.mixin.accessor.InventoryScreenAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager != null && client.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL) {
            client.execute(() -> {
                InventoryScreenAccessor screen = (InventoryScreenAccessor) this;
                RecipeBookWidget recipeBook = screen.getRecipeBook();
                if (recipeBook != null) {
                    ((InventoryScreen) (Object) this).setFocused(recipeBook);
                }
            });
        }
    }
}
