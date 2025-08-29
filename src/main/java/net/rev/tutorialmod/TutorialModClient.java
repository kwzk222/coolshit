package net.rev.tutorialmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.rev.tutorialmod.event.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.Entity;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.text.Text;


public class TutorialModClient implements ClientModInitializer {

    private static boolean modEnabled = true;
    private static KeyBinding toggleKeyBinding;

    private enum SwapAction {
        NONE,
        SWITCH_BACK,
        SWITCH_BACK_ATTACK_MACE
    }

    private int swapCooldown = -1;
    private int originalHotbarSlot = -1;
    private SwapAction nextAction = SwapAction.NONE;
    private Entity targetEntity = null;

    @Override
    public void onInitializeClient() {
        toggleKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.tutorialmod.toggle", // The translation key
                InputUtil.Type.KEYSYM, // The type of input
                GLFW.GLFW_KEY_K, // The default key
                "key.categories.tutorialmod" // The category
        ));

        // Register the totem swap feature (already in your code)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!modEnabled) return;
            if (client.player == null || client.currentScreen == null || client.interactionManager == null) {
                return;
            }
            if (client.player.isCreative()) {
                return;
            }
            if (client.currentScreen instanceof HandledScreen<?> screen) {
                ItemStack offHandStack = client.player.getOffHandStack();

                if (offHandStack.getItem() == Items.TOTEM_OF_UNDYING) {
                    return; // already have totem
                }

                double scaledMouseX = client.mouse.getX() * client.getWindow().getScaledWidth() / client.getWindow().getWidth();
                double scaledMouseY = client.mouse.getY() * client.getWindow().getScaledHeight() / client.getWindow().getHeight();

                int guiLeft = (client.getWindow().getScaledWidth() - 176) / 2;
                int guiTop = (client.getWindow().getScaledHeight() - 166) / 2;

                for (Slot slot : screen.getScreenHandler().slots) {
                    int slotX = guiLeft + slot.x;
                    int slotY = guiTop + slot.y;

                    if (scaledMouseX >= slotX && scaledMouseX < slotX + 16 &&
                            scaledMouseY >= slotY && scaledMouseY < slotY + 16) {

                        ItemStack hoveredStack = slot.getStack();
                        if (hoveredStack.getItem() == Items.TOTEM_OF_UNDYING) {
                            client.interactionManager.clickSlot(
                                    screen.getScreenHandler().syncId,
                                    slot.id,
                                    40,
                                    net.minecraft.screen.slot.SlotActionType.SWAP,
                                    client.player
                            );
                        }
                        break;
                    }
                }
            }
        });

        AttackEntityCallback.EVENT.register((player, entity) -> {
            if (!modEnabled || swapCooldown != -1) {
                return ActionResult.PASS; // Mod disabled or swap already in progress
            }

            if (entity instanceof PlayerEntity) {
                PlayerEntity attackedPlayer = (PlayerEntity) entity;
                boolean isShielding = attackedPlayer.isUsingItem() && attackedPlayer.getActiveItem().getItem() == Items.SHIELD;
                boolean hasArmor = isArmored(attackedPlayer);

                if (isShielding) {
                    int axeSlot = findAxeInHotbar(player);
                    if (axeSlot != -1 && player.getInventory().selectedSlot != axeSlot) {
                        originalHotbarSlot = player.getInventory().selectedSlot;
                        player.getInventory().selectedSlot = axeSlot;
                        targetEntity = entity; // Store entity for potential combo

                        int maceSlot = findMaceInHotbar(player);
                        if (hasArmor && maceSlot != -1) {
                            // Combo case
                            swapCooldown = 1;
                            nextAction = SwapAction.SWITCH_BACK_ATTACK_MACE;
                        } else {
                            // Axe-only case
                            swapCooldown = 5;
                            nextAction = SwapAction.SWITCH_BACK;
                        }
                    }
                } else if (hasArmor) {
                    int maceSlot = findMaceInHotbar(player);
                    if (maceSlot != -1 && player.getInventory().selectedSlot != maceSlot) {
                        originalHotbarSlot = player.getInventory().selectedSlot;
                        player.getInventory().selectedSlot = maceSlot;
                        swapCooldown = 1;
                        nextAction = SwapAction.SWITCH_BACK;
                    }
                }
            }

            return ActionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (swapCooldown > 0) {
                swapCooldown--;
            } else if (swapCooldown == 0) {
                SwapAction action = nextAction;
                nextAction = SwapAction.NONE;
                swapCooldown = -1;

                if (client.player == null) return;

                switch (action) {
                    case SWITCH_BACK:
                        client.player.getInventory().selectedSlot = originalHotbarSlot;
                        break;
                    case SWITCH_BACK_ATTACK_MACE:
                        client.player.getInventory().selectedSlot = originalHotbarSlot;
                        if (client.interactionManager != null && targetEntity != null && targetEntity.isAlive()) {
                            client.interactionManager.attackEntity(client.player, targetEntity);
                            int maceSlot = findMaceInHotbar(client.player);
                            if (maceSlot != -1) {
                                client.player.getInventory().selectedSlot = maceSlot;
                                swapCooldown = 2;
                                nextAction = SwapAction.SWITCH_BACK;
                            }
                        }
                        break;
                    default:
                        // Do nothing for NONE
                        break;
                }
                targetEntity = null; // Clean up stored entity
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKeyBinding.wasPressed()) {
                modEnabled = !modEnabled;
                if (client.player != null) {
                    client.player.sendMessage(Text.of("TutorialMod Features " + (modEnabled ? "Enabled" : "Disabled")), false);
                }
            }
        });
    }

    private boolean isArmored(PlayerEntity player) {
        for (ItemStack armorStack : player.getArmorItems()) {
            if (!armorStack.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private int findAxeInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof AxeItem) {
                return i;
            }
        }
        return -1;
    }

    private int findMaceInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.MACE) {
                return i;
            }
        }
        return -1;
    }
}
