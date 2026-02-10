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
    private int tickCounter = 0;

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

        // Periodic window sync (every 40 frames approx, or use the tick counter)
        // We'll use a frame-based counter for simplicity here, but syncWindowBounds handles the interval.

        long now = System.currentTimeMillis();
        long refreshInterval = 1000 / Math.max(1, TutorialMod.CONFIG.espRefreshRate);
        if (now - lastRefreshTime < refreshInterval) {
            return;
        }
        lastRefreshTime = now;

        vanishedPlayers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdate > 5000);

        // Combined Matrix: Projection * ModelView
        Matrix4f combinedMatrix = new Matrix4f(projectionMatrix).mul(modelViewMatrix);
        updateESP(tickCounter, camera, combinedMatrix);
    }

    public void onTick() {
        tickCounter++;
        if (tickCounter % 20 == 0) { // Every second
            syncWindowBounds();
        }
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
            int ty = (int) top2D.y;

            int boxHeight = (int) (Math.abs(by - ty) * TutorialMod.CONFIG.espBoxScale);
            int boxWidth = (int) (boxHeight * 0.45); // Slightly thinner for players
            int boxX = bx - boxWidth / 2;
            int boxY = ty;

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

        if (vec.w > 0.1f) {
            // Convert to physical pixel coordinates
            float width = (float) client.getWindow().getFramebufferWidth();
            float height = (float) client.getWindow().getFramebufferHeight();

            float x = (vec.x / vec.w + 1.0f) * 0.5f * width;
            float y = (1.0f - vec.y / vec.w) * 0.5f * height;

            // Apply User Calibration
            x = (float) (x * TutorialMod.CONFIG.espScaleFactor);
            y = (float) (y * TutorialMod.CONFIG.espScaleFactor);

            return new Vector4f(x, y, 0, 0);
        }
        return null;
    }

    public void syncWindowBounds() {
        if (net.rev.tutorialmod.TutorialModClient.getESPOverlayManager() == null || !net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().isRunning()) {
            return;
        }

        var window = client.getWindow();
        long handle = window.getHandle();

        // Logical position of content area
        int[] winX = new int[1], winY = new int[1];
        GLFW.glfwGetWindowPos(handle, winX, winY);

        // Physical size
        int w = window.getFramebufferWidth();
        int h = window.getFramebufferHeight();

        // Automatic scaling detection
        double scaleX = (double) w / window.getWidth();
        double scaleY = (double) h / window.getHeight();

        // Calculate Physical X/Y from Logical X/Y
        int x = (int) (winX[0] * scaleX);
        int y = (int) (winY[0] * scaleY);

        // Apply User Offsets and Adjustments
        x += TutorialMod.CONFIG.espOffsetX;
        y += TutorialMod.CONFIG.espOffsetY;
        w += TutorialMod.CONFIG.espWidthAdjust;
        h += TutorialMod.CONFIG.espHeightAdjust;

        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand(
            String.format("WINDOW_SYNC %d,%d,%d,%d", x, y, w, h)
        );

        // Also sync debug mode
        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand("DEBUG " + TutorialMod.CONFIG.espDebugMode);
    }
}
