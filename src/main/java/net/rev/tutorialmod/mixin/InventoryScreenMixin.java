package net.rev.tutorialmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<ScreenHandler> {

    public InventoryScreenMixin(ScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager != null && client.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL) {
            client.execute(() -> {
                for (Element e : children()) {
                    if (e instanceof RecipeBookWidget) {
                        setFocused(e);
                        break;
                    }
                }
            });
        }
    }
}
