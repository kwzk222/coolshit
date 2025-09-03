package net.rev.tutorialmod.humanmove;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.mixin.HandledScreenAccessor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.rev.tutorialmod.humanmove.MathUtils.*;

public final class HumanMoveController {
    private static final HumanMoveController INSTANCE = new HumanMoveController();
    private final Random rnd = new Random();
    private MovementJob job;
    private float lastGuiX = -1, lastGuiY = -1;

    private HumanMoveController() {}

    public static HumanMoveController getInstance() {
        return INSTANCE;
    }

    public void startCameraMove(float targetYaw, float targetPitch, Runnable onDone) {
        var mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p == null) return;

        var params = TutorialMod.CONFIG.movementParams;
        float y0 = wrapDeg(p.getYaw());
        float p0 = clamp(p.getPitch(), params.pitchMin, params.pitchMax);

        var segs = MovementGenerator.planSegments(y0, p0, targetYaw, targetPitch, params, rnd);
        float react = rFloat(params.reactMinMs, params.reactMaxMs, rnd);
        var pc = MovementGenerator.precompute(segs, params, rnd, react);
        MovementGenerator.applyHumanNoiseInPlace(pc, params, rnd, new NoiseState(rnd));

        List<float[]> bufs = new ArrayList<>(2);
        bufs.add(pc.xs);
        bufs.add(pc.ys);
        MovementGenerator.maybeAppendCorrections(targetYaw, targetPitch, bufs, params, rnd);

        job = new MovementJob(MovementJob.Mode.CAMERA, bufs.get(0), bufs.get(1), onDone);
        job.currentX = y0;
        job.currentY = p0;
    }

    public void startGuiMove(float guiX, float guiY, Runnable onDone) {
        var mc = MinecraftClient.getInstance();
        float x0 = getGuiCursorX(mc), y0 = getGuiCursorY(mc);
        var params = TutorialMod.CONFIG.movementParams;

        var segs = MovementGenerator.planSegments(x0, y0, guiX, guiY, params, rnd);
        float react = rFloat(params.reactMinMs, params.reactMaxMs, rnd);
        var pc = MovementGenerator.precompute(segs, params, rnd, react);
        MovementGenerator.applyHumanNoiseInPlace(pc, params, rnd, new NoiseState(rnd));

        List<float[]> bufs = new ArrayList<>(2);
        bufs.add(pc.xs);
        bufs.add(pc.ys);
        MovementGenerator.maybeAppendCorrections(guiX, guiY, bufs, params, rnd);

        job = new MovementJob(MovementJob.Mode.GUI, bufs.get(0), bufs.get(1), onDone);
        job.currentX = x0;
        job.currentY = y0;
    }

    public void renderTick() {
        if (job == null) return;

        if (job.done()) {
            if (job.onDone != null) {
                job.onDone.run();
            }
            job = null;
            return;
        }

        var params = TutorialMod.CONFIG.movementParams;
        int idx = job.i++;
        float tx = job.xs[idx], ty = job.ys[idx];

        job.currentX = lerp(job.currentX, tx, params.blend);
        job.currentY = lerp(job.currentY, ty, params.blend);

        if (job.mode == MovementJob.Mode.CAMERA) {
            applyCamera(job.currentX, job.currentY);
        } else {
            applyGuiCursor(job.currentX, job.currentY);
        }
    }

    private void applyCamera(float yaw, float pitch) {
        var p = MinecraftClient.getInstance().player;
        if (p == null) return;
        var params = TutorialMod.CONFIG.movementParams;
        p.setYaw(wrapDeg(yaw));
        p.setPitch(clamp(pitch, params.pitchMin, params.pitchMax));
    }

    private void applyGuiCursor(float guiX, float guiY) {
        var mc = MinecraftClient.getInstance();
        var win = mc.getWindow();
        double scale = win.getScaleFactor();
        double wx = guiX * scale;
        double wy = guiY * scale;

        lastGuiX = guiX;
        lastGuiY = guiY;
        GLFW.glfwSetCursorPos(win.getHandle(), wx, wy);
    }

    private float getGuiCursorX(MinecraftClient mc) {
        if (lastGuiX != -1) return lastGuiX;
        return mc.getWindow().getScaledWidth() / 2f;
    }

    private float getGuiCursorY(MinecraftClient mc) {
        if (lastGuiY != -1) return lastGuiY;
        return mc.getWindow().getScaledHeight() / 2f;
    }

    public void lookAt(Vec3d target, Runnable onDone) {
        var mc = MinecraftClient.getInstance();
        var p = mc.player;
        if (p == null) return;
        Vec3d eye = p.getEyePos();
        Vec3d d = target.subtract(eye).normalize();
        float yaw = (float) (Math.atan2(d.z, d.x) * -180 / Math.PI) + 90f;
        float pitch = (float) (Math.asin(d.y) * -180 / Math.PI);
        startCameraMove(yaw, pitch, onDone);
    }

    public void moveToSlot(HandledScreen<?> screen, Slot slot, Runnable onDone) {
        HandledScreenAccessor accessor = (HandledScreenAccessor) screen;
        int sx = accessor.getX();
        int sy = accessor.getY();
        int x = sx + slot.x + 8;
        int y = sy + slot.y + 8;
        startGuiMove(x, y, onDone);
    }

    public void lookAt(Vec3d target) {
        lookAt(target, null);
    }

    public void moveToSlot(HandledScreen<?> screen, Slot slot) {
        moveToSlot(screen, slot, null);
    }
}
