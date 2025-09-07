package net.rev.tutorialmod.modules;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.HandledScreenAccessor;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Environment(EnvType.CLIENT)
public class AutoTotem {

    // --- Runtime cached state ---
    private volatile boolean cachedOffhandHasTotem = false;
    private final Set<Integer> cachedHotbarSlotsWithTotems = Collections.synchronizedSet(new LinkedHashSet<>());
    private final AtomicBoolean lastActionWasModSwap = new AtomicBoolean(false);

    // --- Delayed Action State ---
    private int totemSwapSlot = -1;

    public void init() {
        // All event listeners have been removed for a more robust tick-based check.
    }

    public void onTick(MinecraftClient client) {
        if (this.totemSwapSlot != -1) {
            if (client.player != null && client.interactionManager != null) {
                client.interactionManager.clickSlot(0, 36 + this.totemSwapSlot, 40, SlotActionType.SWAP, client.player);
            }
            this.totemSwapSlot = -1;
            return;
        }

        if (client.player == null) return;
        if (!TutorialMod.CONFIG.autoTotemEnabled) return;

        // The hover-swap feature should only run when an inventory screen is open.
        // Checking the currentScreen directly is more robust than using open/close events.
        if (!(client.currentScreen instanceof HandledScreen)) {
            return;
        }
        if (TutorialMod.CONFIG.autoTotemSurvivalOnly && client.player.isCreative()) {
            return;
        }
        if (lastActionWasModSwap.getAndSet(false)) {
            return;
        }

        HandledScreen<?> handledScreen = (HandledScreen<?>) client.currentScreen;
        Slot hoveredSlot = ((HandledScreenAccessor) handledScreen).getFocusedSlot();

        if (hoveredSlot == null || !hoveredSlot.hasStack() || !isTotem(hoveredSlot.getStack())) {
            return;
        }

        if (!offhandHasTotem(client.player)) {
            performSwapToOffhand(client, handledScreen.getScreenHandler(), hoveredSlot.id);
            return;
        }

        Collections.sort(TutorialMod.CONFIG.autoTotemHotbarSlots);
        for (int hotbarSlotIndex : TutorialMod.CONFIG.autoTotemHotbarSlots) {
            if (hotbarSlotIndex < 0 || hotbarSlotIndex > 8) continue;
            if (!hotbarHasTotemAtIndex(client.player, hotbarSlotIndex)) {
                performMoveToHotbar(client, handledScreen.getScreenHandler(), hoveredSlot.id, 36 + hotbarSlotIndex);
                return;
            }
        }
    }

    private void updateCachedState(ClientPlayerEntity player) {
        cachedOffhandHasTotem = offhandHasTotem(player);
        cachedHotbarSlotsWithTotems.clear();
        for (int i = 0; i < 9; i++) {
            if (hotbarHasTotemAtIndex(player, i)) {
                cachedHotbarSlotsWithTotems.add(i);
            }
        }
    }

    private void performSwapToOffhand(MinecraftClient client, ScreenHandler handler, int slotId) {
        if (client.interactionManager == null) return;
        client.interactionManager.clickSlot(handler.syncId, slotId, 40, SlotActionType.SWAP, client.player);
        lastActionWasModSwap.set(true);
    }

    private void performMoveToHotbar(MinecraftClient client, ScreenHandler handler, int sourceSlotId, int targetSlotId) {
        if (client.interactionManager == null) return;
        client.interactionManager.clickSlot(handler.syncId, sourceSlotId, 0, SlotActionType.PICKUP, client.player);
        client.interactionManager.clickSlot(handler.syncId, targetSlotId, 0, SlotActionType.PICKUP, client.player);
        lastActionWasModSwap.set(true);
    }

    public void handleTotemPop() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;

        PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();

        for (int i = 0; i < 9; i++) {
            if (isTotem(client.player.getInventory().getStack(i))) {
                if (inventory.getSelectedSlot() != i) {
                    inventory.setSelectedSlot(i);
                }
                this.totemSwapSlot = i;
                return;
            }
        }
    }

    private boolean isTotem(ItemStack stack) {
        return stack.getItem() == Items.TOTEM_OF_UNDYING;
    }

    private boolean offhandHasTotem(ClientPlayerEntity player) {
        return isTotem(player.getOffHandStack());
    }

    private boolean hotbarHasTotemAtIndex(ClientPlayerEntity player, int hotbarSlot) {
        return isTotem(player.getInventory().getStack(hotbarSlot));
    }
}
