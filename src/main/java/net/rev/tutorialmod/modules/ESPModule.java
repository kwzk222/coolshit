package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import org.joml.Matrix4f;
import org.joml.Vector4f;

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

    public void onRender(RenderTickCounter tickCounter, Camera camera, Matrix4f projMat, Matrix4f viewMat) {
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

        // Clean up old vanished players (not updated for 5 seconds)
        vanishedPlayers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdate > 5000);

        updateESP(tickCounter, camera, projMat, viewMat);
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
            // In 1.21.1, deltas are typically encoded as (short)(delta * 4096)
            double dx = deltaX / 4096.0;
            double dy = deltaY / 4096.0;
            double dz = deltaZ / 4096.0;
            data.pos = data.pos.add(dx, dy, dz);
            data.lastUpdate = System.currentTimeMillis();
        }
    }

    private void updateESP(RenderTickCounter tickCounter, Camera camera, Matrix4f projMat, Matrix4f viewMat) {
        StringBuilder boxesData = new StringBuilder();
        Vec3d cameraPos = camera.getCameraPos();
        float tickDelta = tickCounter.getTickProgress(true);

        // Process visible players
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !player.isAlive() || player.isInvisibleTo(client.player)) continue;

            // Interpolated position
            double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
            double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY());
            double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());

            appendEntityBox(boxesData, player, new Vec3d(x, y, z), projMat, viewMat, cameraPos, "");
        }

        // Process vanished players
        if (TutorialMod.CONFIG.espAntiVanish) {
            for (Map.Entry<Integer, VanishedPlayerData> entry : vanishedPlayers.entrySet()) {
                appendVanishedBox(boxesData, entry.getValue().pos, projMat, viewMat, cameraPos);
            }
        }

        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().updateBoxes(boxesData.toString());
    }

    private void appendEntityBox(StringBuilder data, Entity entity, Vec3d pos, Matrix4f projMat, Matrix4f viewMat, Vec3d cameraPos, String label) {
        double height = entity.getHeight();
        projectAndAppend(data, pos, height, projMat, viewMat, cameraPos, label);
    }

    private void appendVanishedBox(StringBuilder data, Vec3d pos, Matrix4f projMat, Matrix4f viewMat, Vec3d cameraPos) {
        projectAndAppend(data, pos, 1.8, projMat, viewMat, cameraPos, "Vanished");
    }

    private void projectAndAppend(StringBuilder data, Vec3d worldPos, double height, Matrix4f projMat, Matrix4f viewMat, Vec3d cameraPos, String label) {
        Vec3d bottomPos = worldPos.subtract(cameraPos);
        Vec3d topPos = bottomPos.add(0, height, 0);

        Vector4f bottom2D = project(bottomPos, projMat, viewMat);
        Vector4f top2D = project(topPos, projMat, viewMat);

        if (bottom2D != null && top2D != null) {
            int bx = (int) bottom2D.x;
            int by = (int) bottom2D.y;
            int tx = (int) top2D.x;
            int ty = (int) top2D.y;

            int boxHeight = Math.abs(by - ty);
            int boxWidth = (int) (boxHeight * 0.6);
            int boxX = bx - boxWidth / 2;
            int boxY = ty;

            // Simple clipping: if the box is totally off screen, don't send
            if (boxX + boxWidth < 0 || boxX > client.getWindow().getWidth() || boxY + boxHeight < 0 || boxY > client.getWindow().getHeight()) {
                return;
            }

            if (data.length() > 0) data.append(";");
            data.append(boxX).append(",")
                .append(boxY).append(",")
                .append(boxWidth).append(",")
                .append(boxHeight).append(",")
                .append(label);
        }
    }

    private Vector4f project(Vec3d relPos, Matrix4f projMat, Matrix4f viewMat) {
        Vector4f vec = new Vector4f((float)relPos.x, (float)relPos.y, (float)relPos.z, 1.0f);
        vec.mul(viewMat);
        vec.mul(projMat);

        if (vec.w > 0.1f) { // Increased threshold for stability
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
            int x = window.getX();
            int y = window.getY();
            int w = window.getWidth();
            int h = window.getHeight();

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                try {
                    HWND hwnd = new HWND(new Pointer(window.getHandle()));
                    RECT windowRect = new RECT();
                    RECT clientRect = new RECT();
                    if (User32.INSTANCE.GetWindowRect(hwnd, windowRect) && User32.INSTANCE.GetClientRect(hwnd, clientRect)) {
                        int windowWidth = windowRect.right - windowRect.left;
                        int windowHeight = windowRect.bottom - windowRect.top;
                        int clientWidth = clientRect.right - clientRect.left;
                        int clientHeight = clientRect.bottom - clientRect.top;

                        int border = (windowWidth - clientWidth) / 2;
                        int titleBar = windowHeight - clientHeight - border;

                        x = windowRect.left + border;
                        y = windowRect.top + titleBar;
                        w = clientWidth;
                        h = clientHeight;
                    }
                } catch (Throwable t) {
                    // Fallback
                }
            }

            net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand(
                String.format("WINDOW_SYNC %d,%d,%d,%d", x, y, w, h)
            );
        }
    }
}
