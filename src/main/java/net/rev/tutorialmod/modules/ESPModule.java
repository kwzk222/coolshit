package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ESPModule {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Map<Integer, VanishedPlayerData> vanishedPlayers = new ConcurrentHashMap<>();
    private long lastRefreshTime = 0;
    private int lastWinX, lastWinY, lastWinW, lastWinH;

    public static class VanishedPlayerData {
        public Vec3d pos;
        public long lastUpdate;
        public VanishedPlayerData(Vec3d pos) {
            this.pos = pos;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    public void onRender(RenderTickCounter tickCounter, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix) {
        if (!TutorialMod.CONFIG.showESP || client.player == null || client.world == null) {
            vanishedPlayers.clear();
            return;
        }

        syncWindowBoundsIfChanged();

        long now = System.currentTimeMillis();
        long refreshInterval = 1000 / Math.max(1, TutorialMod.CONFIG.espRefreshRate);

        if (now - lastRefreshTime < refreshInterval) {
            return;
        }
        lastRefreshTime = now;

        vanishedPlayers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdate > 5000);

        // Use the game's actual matrices
        Matrix4f combinedMatrix = new Matrix4f(projectionMatrix).mul(modelViewMatrix);
        updateESP(tickCounter, camera, combinedMatrix);
    }

    public void updateVanishedPlayer(int entityId, double x, double y, double z) {
        if (!TutorialMod.CONFIG.espAntiVanish) return;
        if (client.world != null && client.world.getEntityById(entityId) != null) {
            vanishedPlayers.remove(entityId);
            return;
        }
        vanishedPlayers.put(entityId, new VanishedPlayerData(new Vec3d(x, y, z)));
    }

    public void updateVanishedPlayerRelative(int entityId, short deltaX, short deltaY, short deltaZ) {
        if (!TutorialMod.CONFIG.espAntiVanish) return;
        if (client.world != null && client.world.getEntityById(entityId) != null) {
            vanishedPlayers.remove(entityId);
            return;
        }

        VanishedPlayerData data = vanishedPlayers.get(entityId);
        if (data != null) {
            data.pos = data.pos.add(deltaX / 4096.0, deltaY / 4096.0, deltaZ / 4096.0);
            data.lastUpdate = System.currentTimeMillis();
        }
    }

    private void updateESP(RenderTickCounter tickCounter, Camera camera, Matrix4f combinedMatrix) {
        StringBuilder boxesData = new StringBuilder();
        float tickDelta = tickCounter.getTickProgress(true);
        Vec3d cameraPos = camera.getCameraPos();

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !player.isAlive() || player.isInvisibleTo(client.player)) continue;

            double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
            double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY());
            double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());

            appendEntityBox(boxesData, player, new Vec3d(x, y, z).subtract(cameraPos), combinedMatrix, "");
        }

        if (TutorialMod.CONFIG.espAntiVanish) {
            for (Map.Entry<Integer, VanishedPlayerData> entry : vanishedPlayers.entrySet()) {
                appendVanishedBox(boxesData, entry.getValue().pos.subtract(cameraPos), combinedMatrix);
            }
        }

        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().updateBoxes(boxesData.toString());
    }

    private void appendEntityBox(StringBuilder data, Entity entity, Vec3d relPos, Matrix4f combinedMatrix, String label) {
        projectAndAppend(data, relPos, entity.getHeight(), combinedMatrix, label);
    }

    private void appendVanishedBox(StringBuilder data, Vec3d relPos, Matrix4f combinedMatrix) {
        projectAndAppend(data, relPos, 1.8, combinedMatrix, "Vanished");
    }

    private void projectAndAppend(StringBuilder data, Vec3d relPos, double height, Matrix4f combinedMatrix, String label) {
        Vector4f bottom2D = project(relPos, combinedMatrix);
        Vector4f top2D = project(relPos.add(0, height, 0), combinedMatrix);

        if (bottom2D != null && top2D != null) {
            int bx = (int) bottom2D.x;
            int by = (int) bottom2D.y;
            int tx = (int) top2D.x;
            int ty = (int) top2D.y;

            int boxHeight = Math.abs(by - ty);
            int boxWidth = (int) (boxHeight * 0.6);
            int boxX = bx - boxWidth / 2;
            int boxY = Math.min(by, ty);

            if (data.length() > 0) data.append(";");
            data.append(boxX).append(",")
                .append(boxY).append(",")
                .append(boxWidth).append(",")
                .append(boxHeight).append(",")
                .append(label);
        }
    }

    private Vector4f project(Vec3d relPos, Matrix4f combinedMatrix) {
        Vector4f vec = new Vector4f((float)relPos.x, (float)relPos.y, (float)relPos.z, 1.0f);
        combinedMatrix.transform(vec);

        if (vec.w > 0.001f) {
            float x = (vec.x / vec.w + 1.0f) / 2.0f * client.getWindow().getWidth();
            float y = (1.0f - vec.y / vec.w) / 2.0f * client.getWindow().getHeight();
            return new Vector4f(x, y, 0, 0);
        }
        return null;
    }

    private void syncWindowBoundsIfChanged() {
        var window = client.getWindow();
        if (window.getX() != lastWinX || window.getY() != lastWinY || window.getWidth() != lastWinW || window.getHeight() != lastWinH) {
            syncWindowBounds();
            lastWinX = window.getX();
            lastWinY = window.getY();
            lastWinW = window.getWidth();
            lastWinH = window.getHeight();
        }
    }

    public void syncWindowBounds() {
        if (net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().isRunning()) {
            var window = client.getWindow();

            // Re-evaluating alignment.
            // If it's to the left and down:
            // Shifted left = X is too small. Shifted down = Y is too large.
            // If I was using window.getX() + leftBorder, and it was shifted left, maybe window.getX() is already logical client X?
            // Actually, let's try raw window.getX() and window.getY().

            int x = window.getX();
            int y = window.getY();
            int w = window.getWidth();
            int h = window.getHeight();

            net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand(
                String.format("WINDOW_SYNC %d,%d,%d,%d", x, y, w, h)
            );
        }
    }
}
