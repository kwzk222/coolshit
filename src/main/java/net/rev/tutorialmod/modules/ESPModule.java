package net.rev.tutorialmod.modules;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.rev.tutorialmod.TutorialMod;
import net.rev.tutorialmod.TutorialModClient;
import org.joml.Matrix4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ESPModule {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private final Map<Integer, VanishedPlayerData> vanishedPlayers = new ConcurrentHashMap<>();
    private final Map<BlockPos, XRayBlock> xrayBoxes = new ConcurrentHashMap<>();
    private final Map<String, String> texturePathCache = new ConcurrentHashMap<>();
    private long lastXrayScan = 0;
    private final ExecutorService xrayExecutor = Executors.newSingleThreadExecutor();
    private int frameSkipCounter = 0;
    private boolean needsSync = true;
    private final File textureDir;

    public ESPModule() {
        textureDir = new File(System.getProperty("java.io.tmpdir"), "tutorialmod_textures");
        if (!textureDir.exists()) textureDir.mkdirs();
    }

    public static class VanishedPlayerData {
        public Vec3d pos;
        public long lastUpdate;
        public VanishedPlayerData(Vec3d pos) {
            this.pos = pos;
            this.lastUpdate = System.currentTimeMillis();
        }
    }

    public void updateVanishedPlayer(int id, double x, double y, double z) {
        vanishedPlayers.put(id, new VanishedPlayerData(new Vec3d(x, y, z)));
    }

    public void updateVanishedPlayerRelative(int id, short dx, short dy, short dz) {
        VanishedPlayerData data = vanishedPlayers.get(id);
        if (data != null) {
            data.pos = data.pos.add(dx / 4096.0, dy / 4096.0, dz / 4096.0);
            data.lastUpdate = System.currentTimeMillis();
        }
    }

    public void triggerSync() {
        needsSync = true;
    }

    public void onRender(RenderTickCounter tickCounter, Camera camera, Matrix4f projectionMatrix, Matrix4f modelViewMatrix) {
        if (mc.player == null || mc.world == null || !TutorialMod.CONFIG.showESP) {
            if (TutorialModClient.getESPOverlayManager() != null) {
                TutorialModClient.getESPOverlayManager().sendCommand("CLEAR");
            }
            vanishedPlayers.clear();
            xrayBoxes.clear();
            return;
        }

        if (needsSync) {
            syncWindowBounds();
            needsSync = false;
        }

        long now = System.currentTimeMillis();

        int entityCount = 0;
        for (Entity e : mc.world.getEntities()) entityCount++;
        int totalApprox = entityCount + xrayBoxes.size() + vanishedPlayers.size();
        int skipRate = totalApprox > TutorialMod.CONFIG.espMaxBoxes ? (totalApprox / TutorialMod.CONFIG.espMaxBoxes) : 0;
        if (frameSkipCounter < skipRate) {
            frameSkipCounter++;
            return;
        }
        frameSkipCounter = 0;

        if (now - lastXrayScan > 2000) {
            lastXrayScan = now;
            updateXRay();
        }

        vanishedPlayers.entrySet().removeIf(entry -> now - entry.getValue().lastUpdate > 5000);

        Vec3d camPos = camera.getCameraPos();
        float yaw = (float) Math.toRadians(camera.getYaw());
        float pitch = (float) Math.toRadians(camera.getPitch());
        float width = (float) mc.getWindow().getWidth();
        float height = (float) mc.getWindow().getHeight();
        float fov = (float) TutorialMod.CONFIG.espManualFov;
        float aspectRatio = (width / height) * (float)TutorialMod.CONFIG.espAspectRatioScale;

        ESPOverlayManager om = TutorialModClient.getESPOverlayManager();
        if (om == null) return;

        StringBuilder boxesData = new StringBuilder();

        // Entities
        for (Entity entity : mc.world.getEntities()) {
            if (entity == mc.player) continue;
            if (!shouldShow(entity)) continue;

            Vec3d entityPos = new Vec3d(entity.getX(), entity.getY(), entity.getZ());
            double dist = entityPos.distanceTo(camPos);
            if (dist < TutorialMod.CONFIG.espMinRange || dist > TutorialMod.CONFIG.espMaxRange) continue;

            BoxData data = projectBox(entity.getBoundingBox(), camPos, yaw, pitch, fov, aspectRatio, width, height);
            if (data == null) continue;

            int color = getEntityColor(entity);
            String name = shouldShowName(entity) ? entity.getName().getString() : "";
            String distLabel = (entity instanceof PlayerEntity && TutorialMod.CONFIG.espShowDistance && dist >= TutorialMod.CONFIG.espDistanceHideThreshold) ? (int)dist + "m" : "";

            appendBox(boxesData, data, name, color, distLabel, "");
        }

        // Vanished Players
        for (Map.Entry<Integer, VanishedPlayerData> entry : vanishedPlayers.entrySet()) {
            if (mc.world.getEntityById(entry.getKey()) != null) continue;
            Vec3d pos = entry.getValue().pos;
            double dist = pos.distanceTo(camPos);
            if (dist > TutorialMod.CONFIG.espMaxRange) continue;

            Box box = new Box(pos.x - 0.3, pos.y, pos.z - 0.3, pos.x + 0.3, pos.y + 1.8, pos.z + 0.3);
            BoxData data = projectBox(box, camPos, yaw, pitch, fov, aspectRatio, width, height);
            if (data == null) continue;

            appendBox(boxesData, data, "VANISHED", 0xFF0000, (int)dist + "m", "");
        }

        // X-Ray
        for (XRayBlock xb : xrayBoxes.values()) {
            double dist = Math.sqrt(xb.pos.getSquaredDistance(camPos.x, camPos.y, camPos.z));
            if (dist > xb.range) continue;

            BoxData data = projectBox(xb.box, camPos, yaw, pitch, fov, aspectRatio, width, height);
            if (data == null) continue;

            String name = TutorialMod.CONFIG.xrayShowNames ? xb.name : "";
            String tex = TutorialMod.CONFIG.xrayShowTextures ? xb.texturePath : "";

            appendBox(boxesData, data, name, TutorialMod.CONFIG.xrayColor, "", tex);
        }

        om.updateBoxes(boxesData.toString());
    }

    private void appendBox(StringBuilder sb, BoxData data, String label, int color, String distLabel, String texture) {
        if (sb.length() > 0) sb.append(";");
        // Using | as delimiter to avoid issues with paths containing commas
        sb.append(String.format(Locale.ROOT, "%d|%d|%d|%d|%s|%d|%s|%s",
            (int)data.x, (int)data.y, (int)data.w, (int)data.h, label, color, distLabel, texture));
    }

    private BoxData projectBox(Box box, Vec3d camPos, float yaw, float pitch, float fov, float aspectRatio, float width, float height) {
        Vec3d[] corners = new Vec3d[]{
            new Vec3d(box.minX, box.minY, box.minZ), new Vec3d(box.maxX, box.minY, box.minZ),
            new Vec3d(box.minX, box.maxY, box.minZ), new Vec3d(box.maxX, box.maxY, box.minZ),
            new Vec3d(box.minX, box.minY, box.maxZ), new Vec3d(box.maxX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.maxZ), new Vec3d(box.maxX, box.maxY, box.maxZ)
        };

        int[][] edges = {
            {0,1}, {1,5}, {5,4}, {4,0},
            {2,3}, {3,7}, {7,6}, {6,2},
            {0,2}, {1,3}, {4,6}, {5,7}
        };

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean anyVisible = false;
        float near = 0.05f;

        for (int[] edge : edges) {
            Vec3d p1 = corners[edge[0]];
            Vec3d p2 = corners[edge[1]];

            Vec3d r1 = rotate(p1.subtract(camPos), yaw, pitch);
            Vec3d r2 = rotate(p2.subtract(camPos), yaw, pitch);

            if (r1.z < near && r2.z < near) continue;

            if (r1.z >= near && r2.z >= near) {
                Vector2d proj1 = project(r1, fov, aspectRatio, width, height);
                Vector2d proj2 = project(r2, fov, aspectRatio, width, height);
                minX = Math.min(minX, Math.min(proj1.x, proj2.x));
                minY = Math.min(minY, Math.min(proj1.y, proj2.y));
                maxX = Math.max(maxX, Math.max(proj1.x, proj2.x));
                maxY = Math.max(maxY, Math.max(proj1.y, proj2.y));
                anyVisible = true;
            } else {
                Vec3d in = r1.z >= near ? r1 : r2;
                Vec3d out = r1.z < near ? r1 : r2;
                float t = (near - (float)out.z) / (float)(in.z - out.z);
                Vec3d clipped = out.add(in.subtract(out).multiply(t));

                Vector2d projIn = project(in, fov, aspectRatio, width, height);
                Vector2d projClipped = project(clipped, fov, aspectRatio, width, height);
                minX = Math.min(minX, Math.min(projIn.x, projClipped.x));
                minY = Math.min(minY, Math.min(projIn.y, projClipped.y));
                maxX = Math.max(maxX, Math.max(projIn.x, projClipped.x));
                maxY = Math.max(maxY, Math.max(projIn.y, projClipped.y));
                anyVisible = true;
            }
        }

        if (!anyVisible) return null;

        float w = maxX - minX;
        float h = maxY - minY;

        double scale = TutorialMod.CONFIG.espScaleFactor;
        int ox = TutorialMod.CONFIG.espOffsetX;
        int oy = TutorialMod.CONFIG.espOffsetY;
        int wa = TutorialMod.CONFIG.espWidthAdjust;
        int ha = TutorialMod.CONFIG.espHeightAdjust;

        float finalW = (float)(w * scale * TutorialMod.CONFIG.espBoxScale) + wa;
        float finalH = (float)(h * scale * TutorialMod.CONFIG.espBoxScale) + ha;
        float finalX = (float)(minX * scale) + ox + (w - finalW) / 2f;
        float finalY = (float)(minY * scale) + oy + (h - finalH) / 2f;

        return new BoxData(finalX, finalY, finalW, finalH);
    }

    private Vec3d rotate(Vec3d rel, float yaw, float pitch) {
        double x = rel.x * Math.cos(yaw) + rel.z * Math.sin(yaw);
        double z = rel.z * Math.cos(yaw) - rel.x * Math.sin(yaw);
        double y = rel.y;
        double tempZ = z * Math.cos(pitch) + y * Math.sin(pitch);
        y = y * Math.cos(pitch) - z * Math.sin(pitch);
        z = tempZ;
        return new Vec3d(x, y, z);
    }

    private Vector2d project(Vec3d rotated, float fov, float aspectRatio, float width, float height) {
        float f = (float) (1.0 / Math.tan(Math.toRadians(fov) / 2.0));
        float px = (float) (rotated.x * f / aspectRatio / rotated.z);
        float py = (float) (rotated.y * f / rotated.z);
        float screenX = (px + 1) * 0.5f * width;
        float screenY = (1 - py) * 0.5f * height;
        return new Vector2d(screenX, screenY);
    }

    private void updateXRay() {
        if (!TutorialMod.CONFIG.xrayEnabled) {
            xrayBoxes.clear();
            return;
        }

        xrayExecutor.execute(() -> {
            Map<BlockPos, XRayBlock> newBoxes = new HashMap<>();
            BlockPos playerPos = mc.player.getBlockPos();
            int globalRange = TutorialMod.CONFIG.xrayRange;

            Map<Identifier, Integer> rangeMap = new HashMap<>();
            for (String entry : TutorialMod.CONFIG.xrayBlocks) {
                String[] parts = entry.split(":");
                try {
                    if (parts.length >= 3) {
                        rangeMap.put(Identifier.of(parts[0], parts[1]), Integer.parseInt(parts[2]));
                    } else if (parts.length == 2) {
                        if (parts[0].contains("minecraft") || !parts[1].matches("\\d+")) {
                             rangeMap.put(Identifier.of(parts[0], parts[1]), globalRange);
                        } else {
                             rangeMap.put(Identifier.ofVanilla(parts[0]), Integer.parseInt(parts[1]));
                        }
                    } else {
                        rangeMap.put(Identifier.of(parts[0]), globalRange);
                    }
                } catch (Exception ignored) {}
            }

            Set<BlockPos> visited = new HashSet<>();
            int maxR = rangeMap.values().stream().max(Integer::compare).orElse(globalRange);

            for (int dx = -maxR; dx <= maxR; dx++) {
                for (int dy = -maxR; dy <= maxR; dy++) {
                    for (int dz = -maxR; dz <= maxR; dz++) {
                        BlockPos pos = playerPos.add(dx, dy, dz);
                        if (visited.contains(pos)) continue;

                        BlockState state = mc.world.getBlockState(pos);
                        Identifier id = Registries.BLOCK.getId(state.getBlock());
                        if (rangeMap.containsKey(id)) {
                            int r = rangeMap.get(id);
                            if (pos.getSquaredDistance(playerPos) <= r * r) {
                                if (TutorialMod.CONFIG.xrayClumping) {
                                    List<BlockPos> cluster = findCluster(pos, id, visited);
                                    if (!cluster.isEmpty()) {
                                        XRayBlock xb = createXRayBlock(cluster, state, r);
                                        newBoxes.put(xb.pos, xb);
                                    }
                                } else {
                                    visited.add(pos);
                                    XRayBlock xb = new XRayBlock(pos, new Box(pos), state.getBlock().getName().getString(), r, getTexture(state));
                                    newBoxes.put(pos, xb);
                                }
                            }
                        }
                    }
                }
            }
            xrayBoxes.clear();
            xrayBoxes.putAll(newBoxes);
        });
    }

    private List<BlockPos> findCluster(BlockPos start, Identifier targetId, Set<BlockPos> visited) {
        List<BlockPos> cluster = new ArrayList<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.poll();
            cluster.add(pos);

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        BlockPos neighbor = pos.add(dx, dy, dz);
                        if (!visited.contains(neighbor)) {
                            BlockState s = mc.world.getBlockState(neighbor);
                            if (Registries.BLOCK.getId(s.getBlock()).equals(targetId)) {
                                visited.add(neighbor);
                                queue.add(neighbor);
                            }
                        }
                    }
                }
            }
        }
        return cluster;
    }

    private XRayBlock createXRayBlock(List<BlockPos> cluster, BlockState state, int range) {
        double minX = Double.MAX_VALUE, minY = Double.MAX_VALUE, minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (BlockPos p : cluster) {
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX() + 1); maxY = Math.max(maxY, p.getY() + 1); maxZ = Math.max(maxZ, p.getZ() + 1);
        }
        return new XRayBlock(cluster.get(0), new Box(minX, minY, minZ, maxX, maxY, maxZ),
            state.getBlock().getName().getString(), range, getTexture(state));
    }

    private String getTexture(BlockState state) {
        Identifier id = Registries.BLOCK.getId(state.getBlock());
        String key = id.toString();
        if (texturePathCache.containsKey(key)) return texturePathCache.get(key);

        Identifier texRes = Identifier.of(id.getNamespace(), "textures/block/" + id.getPath() + ".png");
        File outFile = new File(textureDir, id.getNamespace() + "_" + id.getPath() + ".png");

        if (!outFile.exists()) {
            mc.getResourceManager().getResource(texRes).ifPresent(res -> {
                try {
                    BufferedImage img = ImageIO.read(res.getInputStream());
                    ImageIO.write(img, "png", outFile);
                } catch (IOException ignored) {}
            });
        }

        if (outFile.exists()) {
            texturePathCache.put(key, outFile.getAbsolutePath());
            return outFile.getAbsolutePath();
        }
        return "";
    }

    private boolean shouldShow(Entity entity) {
        if (entity instanceof PlayerEntity) return TutorialMod.CONFIG.espPlayers;
        if (entity instanceof VillagerEntity) return TutorialMod.CONFIG.espVillagers;
        if (entity instanceof HostileEntity) return TutorialMod.CONFIG.espHostiles;
        if (entity instanceof TameableEntity) return TutorialMod.CONFIG.espTamed;
        if (entity instanceof PassiveEntity) return TutorialMod.CONFIG.espPassives;
        return false;
    }

    private boolean shouldShowName(Entity entity) {
        if (entity instanceof PlayerEntity) return TutorialMod.CONFIG.espShowNamesPlayers;
        if (entity instanceof VillagerEntity) return TutorialMod.CONFIG.espShowNamesVillagers;
        if (entity instanceof HostileEntity) return TutorialMod.CONFIG.espShowNamesHostiles;
        if (entity instanceof TameableEntity) return TutorialMod.CONFIG.espShowNamesTamed;
        if (entity instanceof PassiveEntity) return TutorialMod.CONFIG.espShowNamesPassives;
        return false;
    }

    private int getEntityColor(Entity entity) {
        if (entity instanceof PlayerEntity) {
            if (TutorialMod.CONFIG.teamManager.isTeammate(entity.getName().getString())) return TutorialMod.CONFIG.espColorTeammate;
            return TutorialMod.CONFIG.espColorEnemy;
        }
        if (entity instanceof VillagerEntity) return TutorialMod.CONFIG.espColorVillager;
        if (entity instanceof HostileEntity) return TutorialMod.CONFIG.espColorHostile;
        if (entity instanceof TameableEntity) return TutorialMod.CONFIG.espColorTamed;
        if (entity instanceof PassiveEntity) return TutorialMod.CONFIG.espColorPassive;
        return 0xFFFFFF;
    }

    public void syncWindowBounds() {
        if (TutorialModClient.getESPOverlayManager() == null || !TutorialModClient.getESPOverlayManager().isRunning()) return;
        var window = mc.getWindow();
        TutorialModClient.getESPOverlayManager().sendCommand(String.format(Locale.ROOT, "WINDOW_SYNC %d,%d,%d,%d", window.getX() + TutorialMod.CONFIG.espOffsetX, window.getY() + TutorialMod.CONFIG.espOffsetY, window.getWidth() + TutorialMod.CONFIG.espWidthAdjust, window.getHeight() + TutorialMod.CONFIG.espHeightAdjust));
        TutorialModClient.getESPOverlayManager().sendCommand("DEBUG " + TutorialMod.CONFIG.espDebugMode);
    }

    private static class Vector2d {
        float x, y;
        Vector2d(float x, float y) { this.x = x; this.y = y; }
    }

    private static class BoxData {
        float x, y, w, h;
        BoxData(float x, float y, float w, float h) { this.x = x; this.y = y; this.w = w; this.h = h; }
    }

    private static class XRayBlock {
        BlockPos pos;
        Box box;
        String name;
        int range;
        String texturePath;
        XRayBlock(BlockPos pos, Box box, String name, int range, String texturePath) {
            this.pos = pos; this.box = box; this.name = name; this.range = range; this.texturePath = texturePath;
        }
    }
}
