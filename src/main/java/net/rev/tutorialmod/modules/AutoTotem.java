package net.rev.tutorialmod.modules;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
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
    private final AtomicBoolean inventoryOpen = new AtomicBoolean(false);
    private final AtomicBoolean lastActionWasModSwap = new AtomicBoolean(false);

    // --- Delayed Action State ---
    private int totemSwapSlot = -1;

    public void init() {
        // Register event listeners
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> inventoryOpen.set(false));
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen) {
                onScreenOpened(client, (HandledScreen<?>) screen);
            }
        });

        // Packet listening is now handled by a mixin.

        // Per-tick logic is handled by onTick() called from TutorialModClient
    }

    private void onScreenOpened(MinecraftClient client, HandledScreen<?> screen) {
        inventoryOpen.set(true);
        lastActionWasModSwap.set(false); // Reset on new screen

        // When inventory is opened, immediately do a full state check and cache update
        if (client.player != null) {
            updateCachedState(client.player);
        }

        // Register a closing event for this specific screen instance
        ScreenEvents.remove(screen).register(closedScreen -> {
            inventoryOpen.set(false);
            if (client.player != null) {
                updateCachedState(client.player); // Cache state on close
            }
        });
    }

    public void onTick(MinecraftClient client) {
        // --- Handle Delayed Swap ---
        if (this.totemSwapSlot != -1) {
            if (client.player != null && client.interactionManager != null) {
                // The player's inventory screenhandler is always syncId 0.
                // The hotbar slot ID is 36 + index. The swap button for offhand is 40.
                client.interactionManager.clickSlot(0, 36 + this.totemSwapSlot, 40, SlotActionType.SWAP, client.player);
            }
            this.totemSwapSlot = -1; // Reset after attempt
            return; // Don't process other logic this tick
        }

        // --- Guard Checks ---
        if (client.player == null || !inventoryOpen.get() || !(client.currentScreen instanceof HandledScreen)) {
            return;
        }
        if (TutorialMod.CONFIG.autoTotemSurvivalOnly && client.player.isCreative()) {
            return;
        }
        if (lastActionWasModSwap.getAndSet(false)) {
            return; // Wait one tick after our own action to prevent loops
        }

        // --- Core Logic ---
        HandledScreen<?> handledScreen = (HandledScreen<?>) client.currentScreen;
        Slot hoveredSlot = ((HandledScreenAccessor) handledScreen).getFocusedSlot();

        // Action is only triggered by hovering over a totem
        if (hoveredSlot == null || !hoveredSlot.hasStack() || !isTotem(hoveredSlot.getStack())) {
            return;
        }

        // Find the first empty destination slot
        // 1. Check offhand
        if (!offhandHasTotem(client.player)) {
            performSwapToOffhand(client, handledScreen.getScreenHandler(), hoveredSlot.id);
            return;
        }

        // 2. Check configured totem slots
        Collections.sort(TutorialMod.CONFIG.autoTotemHotbarSlots);
        for (int hotbarSlotIndex : TutorialMod.CONFIG.autoTotemHotbarSlots) {
            if (hotbarSlotIndex < 0 || hotbarSlotIndex > 8) continue; // Ignore invalid slots

            if (!hotbarHasTotemAtIndex(client.player, hotbarSlotIndex)) {
                // Hotbar slot ID in screen handler is 36 + index
                performMoveToHotbar(client, handledScreen.getScreenHandler(), hoveredSlot.id, 36 + hotbarSlotIndex);
                return; // Action taken, end tick
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

    // --- Low-Level Action Stubs (Implement with real inventory clicks) ---

    private void performSwapToOffhand(MinecraftClient client, ScreenHandler handler, int slotId) {
        if (client.interactionManager == null) return;
        // For swapping with offhand, the button is 40.
        client.interactionManager.clickSlot(handler.syncId, slotId, 40, SlotActionType.SWAP, client.player);
        lastActionWasModSwap.set(true);
    }

    private void performMoveToHotbar(MinecraftClient client, ScreenHandler handler, int sourceSlotId, int targetSlotId) {
        if (client.interactionManager == null) return;
        // Pickup from source
        client.interactionManager.clickSlot(handler.syncId, sourceSlotId, 0, SlotActionType.PICKUP, client.player);
        // Place in target
        client.interactionManager.clickSlot(handler.syncId, targetSlotId, 0, SlotActionType.PICKUP, client.player);
        lastActionWasModSwap.set(true);
    }

    public void handleTotemPop() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;

        PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();

        // Find first totem in hotbar
        for (int i = 0; i < 9; i++) {
            if (isTotem(client.player.getInventory().getStack(i))) {
                // If the totem is not already in the selected slot, switch to it for instant visual feedback
                if (inventory.getSelectedSlot() != i) {
                    inventory.setSelectedSlot(i);
                }
                // Schedule the swap to happen on the next tick
                this.totemSwapSlot = i;
                return; // Only handle one pop
            }
        }
    }

    // --- Helper Methods ---

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
