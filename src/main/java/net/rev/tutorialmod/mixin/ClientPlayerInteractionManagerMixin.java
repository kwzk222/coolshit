package net.rev.tutorialmod.mixin;

import net.minecraft.block.AbstractRailBlock;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import net.rev.tutorialmod.event.AttackEntityCallback;
import java.util.Random;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class ClientPlayerInteractionManagerMixin {

    @Shadow private int blockBreakingCooldown;
    @Shadow private float currentBreakingProgress;
    @Shadow private BlockPos currentBreakingPos;

    private static final Random tutorialmod$random = new Random();
    private BlockPos tutorialmod$lastResetPos = null;

    @Inject(at = @At("HEAD"), method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo info) {
        ActionResult result = AttackEntityCallback.EVENT.invoker().interact(player, target);
        if (result == ActionResult.FAIL) {
            info.cancel();
        }
    }

    @Inject(method = "interactBlock", at = @At("TAIL"))
    private void onInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (TutorialMod.CONFIG.tntMinecartPlacementEnabled && cir.getReturnValue().isAccepted()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof AbstractRailBlock) {
                TutorialModClient.setAwaitingRailConfirmation();
            }
        }
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true, require = 0)
    private void onUpdateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        TutorialMod.getAutoToolSwitch().onBlockBreak(pos);

        if (!TutorialMod.CONFIG.masterEnabled || !TutorialMod.CONFIG.miningResetEnabled) {
            return;
        }

        if (!tutorialmod$isMiningResetHotkeyDown()) {
            return;
        }

        // Early Release Simulation
        if (TutorialMod.CONFIG.miningResetSimulateStops) {
            if (currentBreakingPos != null && pos.equals(currentBreakingPos) && !pos.equals(tutorialmod$lastResetPos)) {
                float threshold = (float) TutorialMod.CONFIG.miningResetThreshold;
                if (currentBreakingProgress >= threshold && currentBreakingProgress < 1.0f) {
                    if (tutorialmod$random.nextInt(100) < TutorialMod.CONFIG.miningResetChance) {
                        ((ClientPlayerInteractionManager)(Object)this).cancelBlockBreaking();
                        tutorialmod$lastResetPos = pos;
                        cir.setReturnValue(false);
                        return;
                    }
                }
            }
        }

        // Skip the cooldown (e.g. from a previous block)
        if (blockBreakingCooldown > TutorialMod.CONFIG.miningResetDelay) {
            if (tutorialmod$random.nextInt(100) < TutorialMod.CONFIG.miningResetChance) {
                blockBreakingCooldown = TutorialMod.CONFIG.miningResetDelay;
            }
        }
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("RETURN"), require = 0)
    private void onUpdateBlockBreakingProgressReturn(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() != null && cir.getReturnValue()) {
            // Reset cooldown here to skip the 5-tick delay between blocks
            if (TutorialMod.CONFIG.masterEnabled && TutorialMod.CONFIG.miningResetEnabled) {
                if (tutorialmod$isMiningResetHotkeyDown()) {
                    if (tutorialmod$random.nextInt(100) < TutorialMod.CONFIG.miningResetChance) {
                        blockBreakingCooldown = TutorialMod.CONFIG.miningResetDelay;
                    }
                }
            }
        }
    }

    private boolean tutorialmod$isMiningResetHotkeyDown() {
        MinecraftClient mc = MinecraftClient.getInstance();
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(TutorialMod.CONFIG.miningResetHotkey);
            long handle = mc.getWindow().getHandle();
            if (key.getCategory() == InputUtil.Type.MOUSE) {
                return GLFW.glfwGetMouseButton(handle, key.getCode()) == GLFW.GLFW_PRESS;
            } else {
                return InputUtil.isKeyPressed(handle, key.getCode());
            }
        } catch (Exception e) {
            return false;
        }
    }
}
