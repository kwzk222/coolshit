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

    public void init() {
        // Register event listeners
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> inventoryOpen.set(false));
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof HandledScreen) {
                onScreenOpened(client, (HandledScreen<?>) screen);
            }
        });

        // Listen for totem pop server-side packet
        ClientPlayNetworking.registerGlobalReceiver(EntityStatusS2CPacket.PACKET_ID, (client, handler, buf, responseSender) -> {
            // We must copy the buffer as the packet is processed on the client thread.
            EntityStatusS2CPacket packet = new EntityStatusS2CPacket(buf.copy());
            client.execute(() -> onEntityStatus(client, packet));
        });

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
        if (client.player == null || !inventoryOpen.get()) {
            return; // Only run logic when inventory is open
        }

        // If the last action was our own swap, wait a tick to prevent loops
        if (lastActionWasModSwap.getAndSet(false)) {
            return;
        }

        // Main logic: check for totem needs and opportunities
        checkAndPerformSwaps(client);
    }

    private void checkAndPerformSwaps(MinecraftClient client) {
        if (client.player == null || !(client.currentScreen instanceof HandledScreen)) {
            return;
        }
        HandledScreen<?> handledScreen = (HandledScreen<?>) client.currentScreen;
        ScreenHandler handler = handledScreen.getScreenHandler();

        // 1. Check if offhand needs a totem
        if (!offhandHasTotem(client.player)) {
            Slot hoveredSlot = handledScreen.focusedSlot;
            if (hoveredSlot != null && hoveredSlot.hasStack() && isTotem(hoveredSlot.getStack())) {
                // If hovering over a totem, perform the swap immediately
                performSwapToOffhand(client, handler, hoveredSlot.id);
                return; // Action taken, end tick
            }
        }

        // 2. If offhand is full, try to fill hotbar slots
        if (offhandHasTotem(client.player)) {
            fillHotbarFromInventory(client, handler);
        }
    }

    private void fillHotbarFromInventory(MinecraftClient client, ScreenHandler handler) {
        for (int hotbarSlotIndex : TutorialMod.CONFIG.autoTotemHotbarSlots) {
            if (!hotbarHasTotemAtIndex(client.player, hotbarSlotIndex)) {
                // Find first available totem in main inventory
                for (int i = 9; i < 36; i++) { // Main inventory slots
                    Slot slot = handler.getSlot(i);
                    if (slot != null && slot.hasStack() && isTotem(slot.getStack())) {
                        quickMoveSlot(client, handler, slot.id);
                        return; // One action per tick
                    }
                }
            }
        }
    }

    private void onEntityStatus(MinecraftClient client, EntityStatusS2CPacket packet) {
        if (client.player != null && packet.getStatus() == 35 && packet.getEntity(client.player.getWorld()) == client.player) {
            // Totem popped, update cache immediately
            cachedOffhandHasTotem = false;
            attemptSwapHotbarSlotToMainHand();
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

    private void quickMoveSlot(MinecraftClient client, ScreenHandler handler, int slotId) {
        if (client.interactionManager == null) return;
        // For shift-clicking, the action is QUICK_MOVE and button is 0.
        client.interactionManager.clickSlot(handler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, client.player);
        lastActionWasModSwap.set(true);
    }

    private void attemptSwapHotbarSlotToMainHand() {
        if (!TutorialMod.CONFIG.autoTotemEnableAutoMainToOffhand) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.interactionManager == null) return;

        for (int hotbarSlot : TutorialMod.CONFIG.autoTotemHotbarSlots) {
            if (cachedHotbarSlotsWithTotems.contains(hotbarSlot)) {
                // Player inventory screen handler syncId is 0. Hotbar slots 0-8 are indices 36-44.
                int slotId = 36 + hotbarSlot;
                client.interactionManager.clickSlot(0, slotId, 40, SlotActionType.SWAP, client.player);

                // Update cache immediately
                cachedHotbarSlotsWithTotems.remove(hotbarSlot);
                cachedOffhandHasTotem = true;
                lastActionWasModSwap.set(true);
                return; // Only swap one totem
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
