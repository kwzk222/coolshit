package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Hand;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;

public class PotionModule {

    private int restoreTicks = -1;
    private int originalSlot = -1;

    public void onTick(MinecraftClient client) {
        if (!TutorialMod.CONFIG.potionModuleEnabled || client.player == null || client.interactionManager == null) {
            return;
        }

        handleRestore(client);

        if (!isHotkeyHeld(client)) {
            return;
        }

        if (client.player.getPitch() < TutorialMod.CONFIG.potionActivationPitch) {
            return;
        }

        // 1. Health Check
        if (client.player.getHealth() < TutorialMod.CONFIG.potionHealthThreshold) {
            int slot = findPotion(client, StatusEffects.INSTANT_HEALTH);
            if (slot != -1) {
                usePotion(client, slot, true);
                return;
            }
        }

        // 2. Status Effects Check
        if (checkEffect(client, StatusEffects.STRENGTH, TutorialMod.CONFIG.potionStrengthThreshold)) {
            int slot = findPotion(client, StatusEffects.STRENGTH);
            if (slot != -1) {
                usePotion(client, slot, false);
                return;
            }
        }

        if (checkEffect(client, StatusEffects.SPEED, TutorialMod.CONFIG.potionSpeedThreshold)) {
            int slot = findPotion(client, StatusEffects.SPEED);
            if (slot != -1) {
                usePotion(client, slot, false);
                return;
            }
        }

        if (checkEffect(client, StatusEffects.FIRE_RESISTANCE, TutorialMod.CONFIG.potionFireResThreshold)) {
            int slot = findPotion(client, StatusEffects.FIRE_RESISTANCE);
            if (slot != -1) {
                usePotion(client, slot, false);
                return;
            }
        }
    }

    private boolean isHotkeyHeld(MinecraftClient client) {
        try {
            return InputUtil.isKeyPressed(client.getWindow(), InputUtil.fromTranslationKey(TutorialMod.CONFIG.potionHotkey).getCode());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkEffect(MinecraftClient client, RegistryEntry<StatusEffect> effect, int thresholdTicks) {
        StatusEffectInstance inst = client.player.getStatusEffect(effect);
        if (inst == null) return true;
        return inst.getDuration() < thresholdTicks;
    }

    private int findPotion(MinecraftClient client, RegistryEntry<StatusEffect> effect) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.getItem() == Items.SPLASH_POTION) {
                PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
                if (contents != null) {
                    if (hasEffect(contents, effect)) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private boolean hasEffect(PotionContentsComponent contents, RegistryEntry<StatusEffect> effect) {
        if (contents.potion().isPresent()) {
            for (StatusEffectInstance inst : contents.potion().get().value().getEffects()) {
                if (inst.getEffectType().equals(effect)) return true;
            }
        }
        for (StatusEffectInstance inst : contents.customEffects()) {
            if (inst.getEffectType().equals(effect)) return true;
        }
        return false;
    }

    private void usePotion(MinecraftClient client, int slot, boolean isHealth) {
        PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
        int oldSlot = inventory.getSelectedSlot();

        if (oldSlot != slot) {
            syncSlot(client, slot);
        }

        if (TutorialMod.CONFIG.potionThrow) {
            client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
            client.player.swingHand(Hand.MAIN_HAND);

            if (isHealth && TutorialMod.CONFIG.potionRestoreSlot) {
                this.originalSlot = oldSlot;
                this.restoreTicks = 1; // Restore next tick to ensure item use packet is processed
            }
        }
    }

    private void handleRestore(MinecraftClient client) {
        if (restoreTicks > 0) {
            restoreTicks--;
        } else if (restoreTicks == 0) {
            if (originalSlot != -1) {
                syncSlot(client, originalSlot);
            }
            restoreTicks = -1;
            originalSlot = -1;
        }
    }

    private void syncSlot(MinecraftClient client, int slot) {
        PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
        inventory.setSelectedSlot(slot);
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }
}
