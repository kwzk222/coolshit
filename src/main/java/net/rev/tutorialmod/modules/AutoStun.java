package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.ModConfig;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.PlayerInventoryMixin;

/**
 * Handles the AutoStun feature, which automatically swaps to the correct tool to stun a player.
 */
public class AutoStun {
    // Enum to manage the state of the combat swap
    private enum SwapAction { NONE, SWITCH_BACK, SWITCH_TO_ORIGINAL_THEN_MACE, SWITCH_BACK_FROM_MACE }
    private int swapCooldown = -1;
    private int originalHotbarSlot = -1;
    private SwapAction nextAction = SwapAction.NONE;
    private Entity targetEntity = null;

    /**
     * Called every client tick to handle the combat swap logic.
     * @param client The Minecraft client instance.
     */
    public void onClientTick(MinecraftClient client) {
        handleCombatSwap(client);
    }

    /**
     * Called when the player attacks an entity.
     * @param player The player attacking.
     * @param target The entity being attacked.
     * @return ActionResult.PASS if the attack should proceed, ActionResult.FAIL to cancel.
     */
    public ActionResult onAttackEntity(PlayerEntity player, Entity target) {
        if (!TutorialMod.CONFIG.masterEnabled || swapCooldown != -1) return ActionResult.PASS;
        if (target instanceof PlayerEntity) {
            PlayerEntity attackedPlayer = (PlayerEntity) target;
            boolean isShielding = attackedPlayer.isUsingItem() && attackedPlayer.getActiveItem().getItem() == Items.SHIELD;

            Vec3d selfPos = player.getPos();
            Vec3d targetPos = attackedPlayer.getPos();
            Vec3d targetLookVec = attackedPlayer.getRotationVector();
            Vec3d vecToSelf = selfPos.subtract(targetPos).normalize();
            boolean isFacing = vecToSelf.dotProduct(targetLookVec) > 0;

            boolean hasArmor = isArmored(attackedPlayer);
            PlayerInventoryMixin inventory = (PlayerInventoryMixin) player.getInventory();
            if (isShielding && isFacing && TutorialMod.CONFIG.axeSwapEnabled) {
                if (TutorialMod.CONFIG.axeSwapFailChance > 0 && new java.util.Random().nextInt(100) < TutorialMod.CONFIG.axeSwapFailChance) {
                    return ActionResult.PASS;
                }
                int axeSlot = findAxeInHotbar(player);
                if (axeSlot != -1 && inventory.getSelectedSlot() != axeSlot) {
                    originalHotbarSlot = inventory.getSelectedSlot();
                    inventory.setSelectedSlot(axeSlot);
                    targetEntity = target;
                    int maceSlot = findMaceInHotbar(player);
                    if (hasArmor && maceSlot != -1 && TutorialMod.CONFIG.maceSwapEnabled && player.fallDistance > TutorialMod.CONFIG.minFallDistance) {
                        swapCooldown = TutorialMod.CONFIG.axeToOriginalDelay;
                        nextAction = SwapAction.SWITCH_TO_ORIGINAL_THEN_MACE;
                    } else {
                        swapCooldown = TutorialMod.CONFIG.axeSwapDelay;
                        nextAction = SwapAction.SWITCH_BACK;
                    }
                }
            } else if (hasArmor && TutorialMod.CONFIG.maceSwapEnabled) {
                int maceSlot = findMaceInHotbar(player);
                if (maceSlot != -1 && inventory.getSelectedSlot() != maceSlot) {
                    originalHotbarSlot = inventory.getSelectedSlot();
                    inventory.setSelectedSlot(maceSlot);
                    swapCooldown = TutorialMod.CONFIG.maceSwapDelay;
                    nextAction = SwapAction.SWITCH_BACK;
                }
            }
        }
        return ActionResult.PASS;
    }

    private void handleCombatSwap(MinecraftClient client) {
        if (swapCooldown > 0) {
            swapCooldown--;
        } else if (swapCooldown == 0) {
            SwapAction action = nextAction;
            nextAction = SwapAction.NONE;
            swapCooldown = -1;
            if (client.player == null) return;
            PlayerInventoryMixin inventory = (PlayerInventoryMixin) client.player.getInventory();
            switch (action) {
                case SWITCH_BACK:
                    inventory.setSelectedSlot(originalHotbarSlot);
                    break;
                case SWITCH_TO_ORIGINAL_THEN_MACE:
                    inventory.setSelectedSlot(originalHotbarSlot);
                    if (client.interactionManager != null && targetEntity != null && targetEntity.isAlive()) {
                        client.interactionManager.attackEntity(client.player, targetEntity);
                    }
                    int maceSlot = findMaceInHotbar(client.player);
                    if (maceSlot != -1) {
                        inventory.setSelectedSlot(maceSlot);
                        swapCooldown = TutorialMod.CONFIG.maceToOriginalDelay;
                        nextAction = SwapAction.SWITCH_BACK_FROM_MACE;
                    }
                    break;
                case SWITCH_BACK_FROM_MACE:
                    inventory.setSelectedSlot(originalHotbarSlot);
                    break;
                default: break;
            }
            targetEntity = null;
        }
    }

    private boolean isArmored(PlayerEntity player) {
        if (!player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) return true;
        if (!player.getEquippedStack(EquipmentSlot.CHEST).isEmpty()) return true;
        if (!player.getEquippedStack(EquipmentSlot.LEGS).isEmpty()) return true;
        if (!player.getEquippedStack(EquipmentSlot.FEET).isEmpty()) return true;
        return false;
    }

    private int findAxeInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() instanceof AxeItem) return i;
        }
        return -1;
    }

    private int findMaceInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            if (player.getInventory().getStack(i).getItem() == Items.MACE) return i;
        }
        return -1;
    }
}
