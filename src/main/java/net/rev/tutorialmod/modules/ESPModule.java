package net.rev.tutorialmod.modules;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.platform.win32.WinDef.RECT;
import com.sun.jna.platform.win32.WinDef.POINT;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ESPModule {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Map<Integer, VanishedPlayerData> vanishedPlayers = new ConcurrentHashMap<>();
    private long lastRefreshTime = 0;

    public static class VanishedPlayerData {
        public Vec3d pos;
        public long lastUpdate;
        public VanishedPlayerData(Vec3d pos) {
            this.pos = pos;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    public void onTick() {
        if (!TutorialMod.CONFIG.showESP || client.player == null || client.world == null) {
            vanishedPlayers.clear();
            return;
        }

        // Clean up old vanished players (not updated for 5 seconds)
        long now = System.currentTimeMillis();
        vanishedPlayers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdate > 5000);

        long refreshInterval = 1000 / Math.max(1, TutorialMod.CONFIG.espRefreshRate);
        if (now - lastRefreshTime < refreshInterval) {
            return;
        }
        lastRefreshTime = now;

        updateESP();
    }

    public void updateVanishedPlayer(int entityId, double x, double y, double z) {
        if (!TutorialMod.CONFIG.espAntiVanish) return;
        if (client.world != null && client.world.getEntityById(entityId) != null) {
            vanishedPlayers.remove(entityId);
            return;
        }
        vanishedPlayers.put(entityId, new VanishedPlayerData(new Vec3d(x, y, z)));
    }

    private void updateESP() {
        if (client.player == null || client.world == null) return;

        StringBuilder boxesData = new StringBuilder();
        Camera camera = client.getEntityRenderDispatcher().camera;
        if (camera == null) return;

        Vec3d cameraPos = camera.getCameraPos();

        // Actually, let's use a simpler method if matrices are not easily available in Tick.
        // We can calculate the projection matrix based on FOV, aspect ratio, etc.

        float fov = (float) client.options.getFov().getValue();
        float aspectRatio = (float) client.getWindow().getFramebufferWidth() / client.getWindow().getFramebufferHeight();
        float near = 0.05f;
        float far = client.options.getClampedViewDistance() * 16;

        Matrix4f projMat = new Matrix4f().perspective((float) Math.toRadians(fov), aspectRatio, near, far);

        // ModelView is based on camera rotation
        Matrix4f viewMat = new Matrix4f()
                .rotate((float) Math.toRadians(camera.getPitch()), 1, 0, 0)
                .rotate((float) Math.toRadians(camera.getYaw() + 180.0f), 0, 1, 0);

        // Process visible players
        for (PlayerEntity player : client.world.getPlayers()) {
            if (player == client.player || !player.isAlive() || player.isInvisibleTo(client.player)) continue;

            appendEntityBox(boxesData, player, new Vec3d(player.getX(), player.getY(), player.getZ()), projMat, viewMat, cameraPos, "");
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
        Box box = entity.getBoundingBox();
        double width = (box.maxX - box.minX);
        double height = (box.maxY - box.minY);

        projectAndAppend(data, pos, width, height, projMat, viewMat, cameraPos, label);
    }

    private void appendVanishedBox(StringBuilder data, Vec3d pos, Matrix4f projMat, Matrix4f viewMat, Vec3d cameraPos) {
        projectAndAppend(data, pos, 0.6, 1.8, projMat, viewMat, cameraPos, "Vanished");
    }

    private void projectAndAppend(StringBuilder data, Vec3d worldPos, double width, double height, Matrix4f projMat, Matrix4f viewMat, Vec3d cameraPos, String label) {
        Vec3d relPos = worldPos.subtract(cameraPos);

        // Project the 8 corners of the bounding box to find the 2D screen bounds
        double hw = width / 2.0;
        Vec3d[] corners = {
            relPos.add(-hw, 0, -hw), relPos.add(hw, 0, -hw),
            relPos.add(-hw, 0, hw), relPos.add(hw, 0, hw),
            relPos.add(-hw, height, -hw), relPos.add(hw, height, -hw),
            relPos.add(-hw, height, hw), relPos.add(hw, height, hw)
        };

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean anyInFront = false;

        for (Vec3d corner : corners) {
            Vector4f vec = new Vector4f((float)corner.x, (float)corner.y, (float)corner.z, 1.0f);
            vec.mul(viewMat);
            vec.mul(projMat);

            if (vec.w > 0) {
                anyInFront = true;
                float x = (vec.x / vec.w + 1.0f) / 2.0f * client.getWindow().getWidth();
                float y = (1.0f - vec.y / vec.w) / 2.0f * client.getWindow().getHeight();
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
            }
        }

        if (anyInFront) {
            if (data.length() > 0) data.append(";");
            data.append((int)minX).append(",")
                .append((int)minY).append(",")
                .append((int)(maxX - minX)).append(",")
                .append((int)(maxY - minY)).append(",")
                .append(label);
        }
    }

    public void syncWindowBounds() {
        if (net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().isRunning()) {
            int x = client.getWindow().getX();
            int y = client.getWindow().getY();
            int w = client.getWindow().getWidth();
            int h = client.getWindow().getHeight();

            net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand(
                String.format("WINDOW_SYNC %d,%d,%d,%d", x, y, w, h)
            );
        }
    }
}
