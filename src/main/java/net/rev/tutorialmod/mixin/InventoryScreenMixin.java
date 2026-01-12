package net.rev.tutorialmod.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.world.GameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {

    @Shadow
    protected abstract List<? extends Element> children();

    @Shadow
    public abstract void setFocused(Element focused);

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.interactionManager != null && client.interactionManager.getCurrentGameMode() == GameMode.SURVIVAL) {
            client.execute(() -> {
                for (Element e : this.children()) {
                    if (e instanceof RecipeBookWidget) {
                        this.setFocused(e);
                        break;
                    }
                }
            });
        }
    }
}
