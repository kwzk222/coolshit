package net.rev.tutorialmod.modules.misc;

import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.ClientPlayerInteractionManagerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MiningResetModule {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private enum State {
        IDLE,
        HOLDING,
        RELEASE_ONE_TICK
    }

    private State state = State.IDLE;
    private boolean isForcedRelease = false;
    private boolean blockBrokenThisTick = false;

    public void onBlockBroken() {
        blockBrokenThisTick = true;
    }

    public void tick() {
        if (mc.player == null || mc.interactionManager == null ||
            !TutorialMod.CONFIG.masterEnabled ||
            !TutorialMod.CONFIG.miningResetEnabled) {
            reset();
            return;
        }

        boolean attackPressed = mc.options.attackKey.isPressed();
        float progress = ((ClientPlayerInteractionManagerAccessor) mc.interactionManager)
                .getCurrentBreakingProgress();
        boolean broken = blockBrokenThisTick || progress >= 1.0f;
        blockBrokenThisTick = false;

        switch (state) {
            case IDLE -> {
                if (attackPressed) {
                    state = State.HOLDING;
                }
            }

            case HOLDING -> {
                if (!attackPressed) {
                    reset();
                    return;
                }

                if (broken) {
                    // Force release for 1 tick
                    isForcedRelease = true;
                    mc.options.attackKey.setPressed(false);
                    state = State.RELEASE_ONE_TICK;
                }
            }

            case RELEASE_ONE_TICK -> {
                isForcedRelease = false;
                if (isPhysicalAttackKeyPressed()) {
                    mc.options.attackKey.setPressed(true);
                    state = State.HOLDING;
                } else {
                    reset();
                }
            }
        }
    }

    private boolean isPhysicalAttackKeyPressed() {
        try {
            InputUtil.Key key = InputUtil.fromTranslationKey(mc.options.attackKey.getBoundKeyTranslationKey());
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

    public boolean isForcedRelease() {
        return isForcedRelease;
    }

    public void reset() {
        state = State.IDLE;
        isForcedRelease = false;
    }
}
