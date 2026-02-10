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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ESPModule {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Map<Integer, VanishedPlayerData> vanishedPlayers = new ConcurrentHashMap<>();
    private long lastRefreshTime = 0;
    private boolean needsSync = true;

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

        if (needsSync) {
            syncWindowBounds();
            needsSync = false;
        }

        long now = System.currentTimeMillis();
        long refreshInterval = 1000 / Math.max(1, TutorialMod.CONFIG.espRefreshRate);
        if (now - lastRefreshTime < refreshInterval) {
            return;
        }
        lastRefreshTime = now;

        vanishedPlayers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdate > 5000);

        // Minecraft 1.21.1: clipPos = projectionMatrix * modelViewMatrix * relPos
        Matrix4f combinedMatrix = new Matrix4f(projectionMatrix).mul(modelViewMatrix);
        updateESP(tickCounter, camera, combinedMatrix);
    }

    public void triggerSync() {
        this.needsSync = true;
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
        Vec3d cameraPos = camera.getCameraPos();
        float tickDelta = tickCounter.getTickProgress(true);

        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !player.isAlive() || player.isInvisibleTo(client.player)) continue;

            double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
            double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY());
            double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());

            // Positions are relative to the camera for precision
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
            float bx = bottom2D.x;
            float by = bottom2D.y;
            float ty = top2D.y;

            float boxHeight = Math.abs(by - ty) * (float)TutorialMod.CONFIG.espBoxScale;
            float boxWidth = boxHeight * 0.45f;
            float boxX = bx - boxWidth / 2f;
            float boxY = Math.min(by, ty);

            if (data.length() > 0) data.append(";");
            data.append(String.format("%.4f,%.4f,%.4f,%.4f,%s", boxX, boxY, boxWidth, boxHeight, label));
        }
    }

    private Vector4f project(Vec3d relPos, Matrix4f combinedMatrix) {
        Vector4f vec = new Vector4f((float)relPos.x, (float)relPos.y, (float)relPos.z, 1.0f);
        combinedMatrix.transform(vec);

        if (vec.w > 0.05f) { // Point is in front of camera
            // NDC coordinates (-1 to 1)
            float ndcX = vec.x / vec.w;
            float ndcY = vec.y / vec.w;

            // Screen space percentages (0 to 1)
            float x = (ndcX + 1.0f) * 0.5f;
            float y = (1.0f - ndcY) * 0.5f;

            return new Vector4f(x, y, 0, 0);
        }
        return null;
    }

    public void syncWindowBounds() {
        if (net.rev.tutorialmod.TutorialModClient.getESPOverlayManager() == null || !net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().isRunning()) {
            return;
        }

        var window = client.getWindow();

        // Logical coordinates for Swing (assuming no forced uiScale)
        int x = window.getX();
        int y = window.getY();
        int w = window.getWidth();
        int h = window.getHeight();

        // Apply User Calibration
        x += TutorialMod.CONFIG.espOffsetX;
        y += TutorialMod.CONFIG.espOffsetY;
        w += TutorialMod.CONFIG.espWidthAdjust;
        h += TutorialMod.CONFIG.espHeightAdjust;

        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand(
            String.format("WINDOW_SYNC %d,%d,%d,%d", x, y, w, h)
        );

        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand("DEBUG " + TutorialMod.CONFIG.espDebugMode);
    }
}
