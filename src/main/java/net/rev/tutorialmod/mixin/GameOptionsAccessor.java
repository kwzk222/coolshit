package net.rev.tutorialmod.mixin;

import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GameOptions.class)
public interface GameOptionsAccessor {
    @Accessor("sprintToggled")
    SimpleOption<Boolean> getSprintToggled();

    @Accessor("sneakToggled")
    SimpleOption<Boolean> getSneakToggled();
}
