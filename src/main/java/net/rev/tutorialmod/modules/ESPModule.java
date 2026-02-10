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
import org.joml.Quaternionf;
import org.joml.Vector4f;

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

        // Dynamic throttling
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

        Matrix4f combinedMatrix;
        float fov;
        float aspectRatio;

        if (TutorialMod.CONFIG.espManualProjection) {
            float width = (float) mc.getWindow().getWidth();
            float height = (float) mc.getWindow().getHeight();
            fov = (float) TutorialMod.CONFIG.espManualFov;
            aspectRatio = (width / height) * (float)TutorialMod.CONFIG.espAspectRatioScale;

            Quaternionf viewRot = new Quaternionf(camera.getRotation());
            viewRot.conjugate();
            combinedMatrix = new Matrix4f().rotation(viewRot);
        } else {
            combinedMatrix = new Matrix4f(projectionMatrix).mul(modelViewMatrix);
            fov = -1;
            aspectRatio = -1;
        }

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

            BoxData data = projectBox(entity.getBoundingBox(), camPos, combinedMatrix, fov, aspectRatio);
            if (data == null) continue;

            int color = getEntityColor(entity);
            String name = shouldShowName(entity) ? entity.getName().getString() : "";
            String distLabel = (entity instanceof PlayerEntity && TutorialMod.CONFIG.espShowDistance && dist >= TutorialMod.CONFIG.espDistanceHideThreshold) ? (int)dist + "m" : "";

            appendBox(boxesData, data, name, color, distLabel, "");
        }

        // Vanished Players
        for (Map.Entry<Integer, VanishedPlayerData> entry : vanishedPlayers.entrySet()) {
            // Refinement: Skip if entity is already known to world
            if (mc.world.getEntityById(entry.getKey()) != null) continue;

            Vec3d pos = entry.getValue().pos;
            double dist = pos.distanceTo(camPos);
            if (dist > TutorialMod.CONFIG.espMaxRange) continue;

            Box box = new Box(pos.x - 0.3, pos.y, pos.z - 0.3, pos.x + 0.3, pos.y + 1.8, pos.z + 0.3);
            BoxData data = projectBox(box, camPos, combinedMatrix, fov, aspectRatio);
            if (data == null) continue;

            appendBox(boxesData, data, "VANISHED", 0xFF0000, (int)dist + "m", "");
        }

        // X-Ray
        for (XRayBlock xb : xrayBoxes.values()) {
            double dist = Math.sqrt(xb.pos.getSquaredDistance(camPos.x, camPos.y, camPos.z));
            if (dist > xb.range) continue;

            BoxData data = projectBox(xb.box, camPos, combinedMatrix, fov, aspectRatio);
            if (data == null) continue;

            String name = TutorialMod.CONFIG.xrayShowNames ? xb.name : "";
            String tex = TutorialMod.CONFIG.xrayShowTextures ? xb.texturePath : "";

            appendBox(boxesData, data, name, TutorialMod.CONFIG.xrayColor, "", tex);
        }

        om.updateBoxes(boxesData.toString());
    }

    private void appendBox(StringBuilder sb, BoxData data, String label, int color, String distLabel, String texture) {
        if (sb.length() > 0) sb.append(";");
        sb.append(String.format(Locale.ROOT, "%.6f,%.6f,%.6f,%.6f,%s,%d,%s,%s",
            data.x, data.y, data.w, data.h, label, color, distLabel, texture));
    }

    private BoxData projectBox(Box box, Vec3d camPos, Matrix4f combinedMatrix, float fov, float aspectRatio) {
        Vec3d[] corners = new Vec3d[]{
            new Vec3d(box.minX, box.minY, box.minZ), new Vec3d(box.maxX, box.minY, box.minZ),
            new Vec3d(box.minX, box.maxY, box.minZ), new Vec3d(box.maxX, box.maxY, box.minZ),
            new Vec3d(box.minX, box.minY, box.maxZ), new Vec3d(box.maxX, box.minY, box.maxZ),
            new Vec3d(box.minX, box.maxY, box.maxZ), new Vec3d(box.maxX, box.maxY, box.maxZ)
        };

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean anyInFront = false;

        for (Vec3d corner : corners) {
            Vector2d p;
            if (TutorialMod.CONFIG.espManualProjection) {
                p = projectManual(corner, camPos, combinedMatrix, fov, aspectRatio);
            } else {
                p = projectAuto(corner, camPos, combinedMatrix);
            }

            if (p != null) {
                anyInFront = true;
                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
            }
        }

        if (!anyInFront) return null;

        float w = maxX - minX;
        float h = maxY - minY;

        float finalW = w * (float)TutorialMod.CONFIG.espBoxScale;
        float finalH = h * (float)TutorialMod.CONFIG.espBoxScale;
        float finalX = minX + (w - finalW) / 2f;
        float finalY = minY + (h - finalH) / 2f;

        return new BoxData(finalX, finalY, finalW, finalH);
    }

    private Vector2d projectAuto(Vec3d pos, Vec3d camPos, Matrix4f combinedMatrix) {
        Vec3d rel = pos.subtract(camPos);
        Vector4f vec = new Vector4f((float)rel.x, (float)rel.y, (float)rel.z, 1.0f);
        combinedMatrix.transform(vec);

        if (vec.w < 0.05f) return null;

        float x = ((vec.x / vec.w) + 1.0f) * 0.5f;
        float y = (1.0f - (vec.y / vec.w)) * 0.5f;

        return new Vector2d(x, y);
    }

    private Vector2d projectManual(Vec3d pos, Vec3d camPos, Matrix4f rotationMatrix, float fov, float aspectRatio) {
        Vec3d rel = pos.subtract(camPos);
        Vector4f vec = new Vector4f((float)rel.x, (float)rel.y, (float)rel.z, 1.0f);
        rotationMatrix.transform(vec);

        if (vec.z < 0.05f) return null;

        float f = (float) (1.0 / Math.tan(Math.toRadians(fov) / 2.0));
        float px = (vec.x * f / aspectRatio / vec.z);
        float py = (vec.y * f / vec.z);

        float x = (px + 1) * 0.5f;
        float y = (1 - py) * 0.5f;

        return new Vector2d(x, y);
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
