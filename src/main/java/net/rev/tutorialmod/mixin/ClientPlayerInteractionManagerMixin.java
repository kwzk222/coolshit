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
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import net.rev.tutorialmod.event.AttackEntityCallback;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {

    @Inject(at = @At("HEAD"), method = "attackEntity(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/entity/Entity;)V", cancellable = true)
    private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo info) {
        ActionResult result = AttackEntityCallback.EVENT.invoker().interact(player, target);
        if (result == ActionResult.FAIL) {
            info.cancel();
        }
    }

    private boolean isPlacingRail = false;

    @Inject(method = "interactBlock", at = @At("HEAD"))
    private void tutorialmod$preInteract(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        ItemStack stack = player.getStackInHand(hand);
        if (stack.getItem() instanceof BlockItem blockItem
            && blockItem.getBlock() instanceof AbstractRailBlock) {

            isPlacingRail = true;
        }
    }

    @Inject(method = "interactBlock", at = @At("TAIL"))
    private void tutorialmod$postInteract(
            ClientPlayerEntity player,
            Hand hand,
            BlockHitResult hitResult,
            CallbackInfoReturnable<ActionResult> cir
    ) {
        if (isPlacingRail && cir.getReturnValue().isAccepted()) {
            TutorialModClient.onRailPlacedClient(hitResult.getBlockPos());
        }
        isPlacingRail = false;
    }

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBlockBreakingProgress(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        TutorialMod.getAutoToolSwitch().onBlockBreak(pos);
    }
}
