package net.rev.tutorialmod.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;

public interface AttackEntityCallback {
    Event<AttackEntityCallback> EVENT = EventFactory.createArrayBacked(AttackEntityCallback.class,
            (listeners) -> (player, entity) -> {
                for (AttackEntityCallback listener : listeners) {
                    ActionResult result = listener.interact(player, entity);

                    if (result != ActionResult.PASS) {
                        return result;
                    }
                }

                return ActionResult.PASS;
            });

    ActionResult interact(PlayerEntity player, Entity entity);
}
