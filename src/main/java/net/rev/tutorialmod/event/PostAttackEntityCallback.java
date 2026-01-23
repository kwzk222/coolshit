package net.rev.tutorialmod.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public interface PostAttackEntityCallback {
    Event<PostAttackEntityCallback> EVENT = EventFactory.createArrayBacked(PostAttackEntityCallback.class,
            (listeners) -> (player, entity) -> {
                for (PostAttackEntityCallback listener : listeners) {
                    listener.interact(player, entity);
                }
            });

    void interact(PlayerEntity player, Entity entity);
}
