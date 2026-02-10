package net.rev.tutorialmod.modules;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.rev.tutorialmod.TutorialMod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ESPModule {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Map<Integer, VanishedPlayerData> vanishedPlayers = new ConcurrentHashMap<>();
    private final List<BlockPos> xrayPositions = new CopyOnWriteArrayList<>();
    private long lastRefreshTime = 0;
    private long lastXrayScanTime = 0;
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
            xrayPositions.clear();
            return;
        }

        if (needsSync) {
            syncWindowBounds();
            needsSync = false;
        }

        long now = System.currentTimeMillis();

        // Handle X-Ray Scanning (Main thread but periodic)
        if (TutorialMod.CONFIG.xrayEnabled && now - lastXrayScanTime > 1000) {
            lastXrayScanTime = now;
            scanXray();
        } else if (!TutorialMod.CONFIG.xrayEnabled) {
            xrayPositions.clear();
        }

        long refreshInterval = 1000 / Math.max(1, TutorialMod.CONFIG.espRefreshRate);
        if (now - lastRefreshTime < refreshInterval) {
            return;
        }
        lastRefreshTime = now;

        vanishedPlayers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdate > 5000);

        Matrix4f combinedMatrix;

        if (TutorialMod.CONFIG.espManualProjection) {
            float fov = (float) TutorialMod.CONFIG.espManualFov;
            float aspect = (float) client.getWindow().getWidth() / (float) client.getWindow().getHeight();
            float near = 0.05f;
            float far = 1000f;

            Matrix4f manualProj = new Matrix4f().perspective((float)Math.toRadians(fov), aspect, near, far);
            Matrix4f manualView = new Matrix4f()
                .rotateX((float)Math.toRadians(camera.getPitch()))
                .rotateY((float)Math.toRadians(camera.getYaw() + 180.0f));

            combinedMatrix = manualProj.mul(manualView);
        } else {
            Quaternionf viewRot = new Quaternionf(camera.getRotation());
            viewRot.conjugate();
            Matrix4f viewMatrix = new Matrix4f().rotation(viewRot);
            combinedMatrix = new Matrix4f(projectionMatrix).mul(viewMatrix);
        }

        updateESP(tickCounter, camera, combinedMatrix);
    }

    private void scanXray() {
        if (client.player == null || client.world == null) return;

        List<BlockPos> found = new ArrayList<>();
        BlockPos playerPos = client.player.getBlockPos();
        World world = client.world;
        int range = TutorialMod.CONFIG.xrayRange;
        List<String> targets = TutorialMod.CONFIG.xrayBlocks;

        // Simple scan on main thread.
        // For range 32, this is ~274k blocks.
        // Optimization: only check loaded chunks.
        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                BlockPos column = playerPos.add(x, 0, z);
                if (!world.isChunkLoaded(column.getX() >> 4, column.getZ() >> 4)) continue;

                for (int y = -range; y <= range; y++) {
                    BlockPos pos = column.add(0, y, 0);
                    Block block = world.getBlockState(pos).getBlock();
                    String id = Registries.BLOCK.getId(block).toString();
                    if (targets.contains(id)) {
                        found.add(pos);
                    }
                }
            }
        }
        xrayPositions.clear();
        xrayPositions.addAll(found);
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

        // 1. Entities
        for (Entity entity : client.world.getEntities()) {
            if (entity == client.player || !entity.isAlive()) continue;

            double dist = entity.distanceTo(client.player);
            if (dist < TutorialMod.CONFIG.espMinRange || dist > TutorialMod.CONFIG.espMaxRange) continue;

            int color = -1;
            String label = "";
            String distLabel = "";

            if (entity instanceof PlayerEntity player) {
                if (!TutorialMod.CONFIG.espPlayers || player.isInvisibleTo(client.player)) continue;
                if (TutorialMod.CONFIG.espShowNamesPlayers) label = player.getName().getString();
                if (TutorialMod.CONFIG.espShowDistance && dist >= TutorialMod.CONFIG.espDistanceHideThreshold) {
                    distLabel = String.format(Locale.ROOT, "%db", (int)dist);
                }
                color = TutorialMod.CONFIG.teamManager.isTeammate(player.getName().getString())
                        ? TutorialMod.CONFIG.espColorTeammate : TutorialMod.CONFIG.espColorEnemy;
            } else if (entity instanceof VillagerEntity) {
                if (!TutorialMod.CONFIG.espVillagers) continue;
                if (TutorialMod.CONFIG.espShowNamesVillagers) label = entity.getName().getString();
                color = TutorialMod.CONFIG.espColorVillager;
            } else if (entity instanceof TameableEntity tameable && tameable.isTamed()) {
                if (!TutorialMod.CONFIG.espTamed) continue;
                if (TutorialMod.CONFIG.espShowNamesTamed) label = entity.getName().getString();
                color = TutorialMod.CONFIG.espColorTamed;
            } else if (entity instanceof Monster) {
                if (!TutorialMod.CONFIG.espHostiles) continue;
                if (TutorialMod.CONFIG.espShowNamesHostiles) label = entity.getName().getString();
                color = TutorialMod.CONFIG.espColorHostile;
            } else if (entity instanceof PassiveEntity) {
                if (!TutorialMod.CONFIG.espPassives) continue;
                if (TutorialMod.CONFIG.espShowNamesPassives) label = entity.getName().getString();
                color = TutorialMod.CONFIG.espColorPassive;
            }

            if (color != -1) {
                double x = MathHelper.lerp(tickDelta, entity.lastRenderX, entity.getX());
                double y = MathHelper.lerp(tickDelta, entity.lastRenderY, entity.getY());
                double z = MathHelper.lerp(tickDelta, entity.lastRenderZ, entity.getZ());

                Vec3d relPos = new Vec3d(x, y, z).subtract(cameraPos);
                projectAndAppend(boxesData, relPos, entity.getHeight(), combinedMatrix, label, color, distLabel);
            }
        }

        // 2. Vanished Players
        if (TutorialMod.CONFIG.espAntiVanish) {
            for (Map.Entry<Integer, VanishedPlayerData> entry : vanishedPlayers.entrySet()) {
                Vec3d relPos = entry.getValue().pos.subtract(cameraPos);
                projectAndAppend(boxesData, relPos, 1.8, combinedMatrix, "Vanished", TutorialMod.CONFIG.espColorEnemy, "");
            }
        }

        // 3. X-Ray Blocks
        if (TutorialMod.CONFIG.xrayEnabled) {
            int color = TutorialMod.CONFIG.xrayColor;
            boolean showNames = TutorialMod.CONFIG.xrayShowNames;
            for (BlockPos pos : xrayPositions) {
                Vec3d relPos = new Vec3d(pos.getX(), pos.getY(), pos.getZ()).subtract(cameraPos);
                String label = showNames ? Registries.BLOCK.getId(client.world.getBlockState(pos).getBlock()).getPath() : "";
                projectAndAppend(boxesData, relPos, 1.0, combinedMatrix, label, color, "");
            }
        }

        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().updateBoxes(boxesData.toString());
    }

    private void projectAndAppend(StringBuilder data, Vec3d relPos, double height, Matrix4f combinedMatrix, String label, int color, String distLabel) {
        Vector4f clipFeet = new Vector4f((float)relPos.x, (float)relPos.y, (float)relPos.z, 1.0f);
        combinedMatrix.transform(clipFeet);

        Vector4f clipHead = new Vector4f((float)relPos.x, (float)relPos.y + (float)height, (float)relPos.z, 1.0f);
        combinedMatrix.transform(clipHead);

        float epsilon = 0.01f;
        boolean feetInFront = clipFeet.w > epsilon;
        boolean headInFront = clipHead.w > epsilon;

        if (!feetInFront && !headInFront) return;

        // Clip to near plane
        if (!feetInFront) {
            float t = (epsilon - clipFeet.w) / (clipHead.w - clipFeet.w);
            clipFeet.lerp(clipHead, t);
            clipFeet.w = epsilon;
        } else if (!headInFront) {
            float t = (epsilon - clipHead.w) / (clipFeet.w - clipHead.w);
            clipHead.lerp(clipFeet, t);
            clipHead.w = epsilon;
        }

        // Perspective divide
        float fovScale = TutorialMod.CONFIG.espManualProjection ? (float)TutorialMod.CONFIG.espFovScale : 1.0f;
        float aspectScale = TutorialMod.CONFIG.espManualProjection ? (float)TutorialMod.CONFIG.espAspectRatioScale : 1.0f;

        float bx = ((clipFeet.x / clipFeet.w) * fovScale * aspectScale + 1.0f) * 0.5f;
        float by = (1.0f - (clipFeet.y / clipFeet.w) * fovScale) * 0.5f;

        float tx = ((clipHead.x / clipHead.w) * fovScale * aspectScale + 1.0f) * 0.5f;
        float ty = (1.0f - (clipHead.y / clipHead.w) * fovScale) * 0.5f;

        float boxHeight = Math.abs(by - ty) * (float)TutorialMod.CONFIG.espBoxScale;
        float boxWidth = boxHeight * (float)TutorialMod.CONFIG.espBoxWidthFactor;
        float boxX = bx - boxWidth / 2f;
        float boxY = Math.min(by, ty);

        if (data.length() > 0) data.append(";");
        data.append(String.format(Locale.ROOT, "%.4f,%.4f,%.4f,%.4f,%s,%d,%s", boxX, boxY, boxWidth, boxHeight, label, color, distLabel));
    }

    public void syncWindowBounds() {
        if (net.rev.tutorialmod.TutorialModClient.getESPOverlayManager() == null || !net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().isRunning()) {
            return;
        }

        var window = client.getWindow();
        int x = window.getX();
        int y = window.getY();
        int w = window.getWidth();
        int h = window.getHeight();

        x += TutorialMod.CONFIG.espOffsetX;
        y += TutorialMod.CONFIG.espOffsetY;
        w += TutorialMod.CONFIG.espWidthAdjust;
        h += TutorialMod.CONFIG.espHeightAdjust;

        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand(
            String.format(Locale.ROOT, "WINDOW_SYNC %d,%d,%d,%d", x, y, w, h)
        );

        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand("DEBUG " + TutorialMod.CONFIG.espDebugMode);
    }
}
