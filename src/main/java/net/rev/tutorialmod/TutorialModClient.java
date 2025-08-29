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
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import net.minecraft.text.Text;


public class TutorialModClient implements ClientModInitializer {

    private static TutorialModClient instance;

    private enum SwapAction {
        NONE,
        SWITCH_BACK,
        SWITCH_BACK_ATTACK_MACE
    }

    private int swapCooldown = -1;
    private int originalHotbarSlot = -1;
    private SwapAction nextAction = SwapAction.NONE;
    private Entity targetEntity = null;

    private enum PlacementAction {
        NONE,
        PLACE_TNT_MINECART
    }

    private int placementCooldown = -1;
    private PlacementAction nextPlacementAction = PlacementAction.NONE;
    private BlockPos railPos = null;

    public static int awaitingRailConfirmationCooldown = -1;

    @Override
    public void onInitializeClient() {
        instance = this;

        // Register the totem swap feature (already in your code)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!TutorialMod.CONFIG.totemSwapEnabled) return;
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
            if (swapCooldown != -1) {
                return ActionResult.PASS; // Swap already in progress
            }

            if (entity instanceof PlayerEntity) {
                PlayerEntity attackedPlayer = (PlayerEntity) entity;
                boolean isShielding = attackedPlayer.isUsingItem() && attackedPlayer.getActiveItem().getItem() == Items.SHIELD;
                boolean hasArmor = isArmored(attackedPlayer);

                if (isShielding && TutorialMod.CONFIG.axeSwapEnabled) {
                    int axeSlot = findAxeInHotbar(player);
                    if (axeSlot != -1 && player.getInventory().selectedSlot != axeSlot) {
                        originalHotbarSlot = player.getInventory().selectedSlot;
                        player.getInventory().selectedSlot = axeSlot;
                        targetEntity = entity; // Store entity for potential combo

                        int maceSlot = findMaceInHotbar(player);
                        if (hasArmor && maceSlot != -1 && TutorialMod.CONFIG.maceSwapEnabled) {
                            // Combo case
                            swapCooldown = TutorialMod.CONFIG.comboSwapDelay;
                            nextAction = SwapAction.SWITCH_BACK_ATTACK_MACE;
                        } else {
                            // Axe-only case
                            swapCooldown = TutorialMod.CONFIG.axeSwapDelay;
                            nextAction = SwapAction.SWITCH_BACK;
                        }
                    }
                } else if (hasArmor && TutorialMod.CONFIG.maceSwapEnabled) {
                    int maceSlot = findMaceInHotbar(player);
                    if (maceSlot != -1 && player.getInventory().selectedSlot != maceSlot) {
                        originalHotbarSlot = player.getInventory().selectedSlot;
                        player.getInventory().selectedSlot = maceSlot;
                        swapCooldown = TutorialMod.CONFIG.maceSwapDelay;
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
                                swapCooldown = TutorialMod.CONFIG.postComboAxeSwapDelay;
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
            if (placementCooldown > 0) {
                placementCooldown--;
            } else if (placementCooldown == 0) {
                PlacementAction action = nextPlacementAction;
                nextPlacementAction = PlacementAction.NONE;
                placementCooldown = -1;

                if (client.player == null || client.interactionManager == null) return;

                switch (action) {
                    case PLACE_TNT_MINECART:
                        HitResult crosshairTarget = client.crosshairTarget;
                        if (crosshairTarget != null && crosshairTarget.getType() == HitResult.Type.BLOCK && ((BlockHitResult) crosshairTarget).getBlockPos().equals(railPos)) {
                            client.player.getInventory().selectedSlot = findTntMinecartInHotbar(client.player);

                            BlockHitResult hitResult = new BlockHitResult(
                                    railPos.toCenterPos(),
                                    Direction.UP,
                                    railPos,
                                    false
                            );
                            client.interactionManager.interactBlock(client.player, Hand.MAIN_HAND, hitResult);
                        }

                        placementCooldown = -1; // No need to switch back, so we're done.
                        railPos = null;
                        break;
                    default:
                        break;
                }
            }
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (awaitingRailConfirmationCooldown > 0) {
                awaitingRailConfirmationCooldown--;
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

    private int findTntMinecartInHotbar(PlayerEntity player) {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() == Items.TNT_MINECART) {
                return i;
            }
        }
        return -1;
    }

    public static void setAwaitingRailConfirmation() {
        awaitingRailConfirmationCooldown = 2;
    }

    public static void confirmRailPlacement(BlockPos pos, BlockState state) {
        if (awaitingRailConfirmationCooldown > 0 && state.getBlock() instanceof net.minecraft.block.RailBlock) {
            if (instance != null) {
                instance.startRailPlacement(pos);
            }
            awaitingRailConfirmationCooldown = -1;
        }
    }

    public void startRailPlacement(BlockPos pos) {
        if (placementCooldown != -1) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int tntMinecartSlot = findTntMinecartInHotbar(client.player);
        if (tntMinecartSlot != -1) {
            this.railPos = pos;
            this.placementCooldown = 1;
            this.nextPlacementAction = PlacementAction.PLACE_TNT_MINECART;
        }
    }
}
