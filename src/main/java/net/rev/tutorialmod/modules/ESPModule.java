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
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.rev.tutorialmod.TutorialMod;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector4f;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class ESPModule {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final Map<Integer, VanishedPlayerData> vanishedPlayers = new ConcurrentHashMap<>();
    private final List<XRayEntry> xrayEntries = new CopyOnWriteArrayList<>();
    private long lastRefreshTime = 0;

    public static class XRayEntry {
        public Vec3d pos;
        public String label;
        public String texture;
        public XRayEntry(Vec3d pos, String label, String texture) {
            this.pos = pos;
            this.label = label;
            this.texture = texture;
        }
    }
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
            xrayEntries.clear();
            return;
        }

        if (TutorialMod.CONFIG.espHideInMenus && client.currentScreen != null) {
            net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand("CLEAR");
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
            xrayEntries.clear();
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

    private final Set<String> extractedTextures = new HashSet<>();
    private static final String TEXTURE_DIR = "tutorialmod_textures";

    private void extractTexture(String blockId) {
        if (extractedTextures.contains(blockId)) return;

        try {
            String path = blockId.contains(":") ? blockId.split(":")[1] : blockId;
            Identifier textureId = Identifier.of("minecraft", "textures/block/" + path + ".png");

            // Special cases for chests
            if (path.equals("chest")) {
                textureId = Identifier.of("minecraft", "textures/entity/chest/normal.png");
            } else if (path.equals("ender_chest")) {
                textureId = Identifier.of("minecraft", "textures/entity/chest/ender.png");
            } else if (path.equals("trapped_chest")) {
                textureId = Identifier.of("minecraft", "textures/entity/chest/trapped.png");
            }

            // Try to find the resource
            var resource = client.getResourceManager().getResource(textureId);
            if (resource.isEmpty()) {
                // Try item texture if block fails
                textureId = Identifier.of("minecraft", "textures/item/" + path + ".png");
                resource = client.getResourceManager().getResource(textureId);
            }

            if (resource.isEmpty()) {
                // Try with _front, _side, etc. if it's a common multi-sided block
                String[] suffixes = {"_front", "_side", "_top", "_bottom", "_outside"};
                for (String suffix : suffixes) {
                    textureId = Identifier.of("minecraft", "textures/block/" + path + suffix + ".png");
                    resource = client.getResourceManager().getResource(textureId);
                    if (resource.isPresent()) break;
                }
            }

            if (resource.isPresent()) {
                File dir = new File(TEXTURE_DIR);
                if (!dir.exists()) dir.mkdirs();

                File outFile = new File(dir, path + ".png");
                try (InputStream is = resource.get().getInputStream()) {
                    BufferedImage image = ImageIO.read(is);
                    if (image != null) {
                        ImageIO.write(image, "png", outFile);
                        extractedTextures.add(blockId);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scanXray() {
        if (client.player == null || client.world == null) return;

        Map<String, List<BlockPos>> foundByBlock = new HashMap<>();
        BlockPos playerPos = client.player.getBlockPos();
        World world = client.world;
        int range = TutorialMod.CONFIG.xrayRange;
        List<String> targets = TutorialMod.CONFIG.xrayBlocks;
        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                int cx = (playerPos.getX() + x) >> 4;
                int cz = (playerPos.getZ() + z) >> 4;
                if (!world.isChunkLoaded(cx, cz)) continue;

                for (int y = -range; y <= range; y++) {
                    mutable.set(playerPos.getX() + x, playerPos.getY() + y, playerPos.getZ() + z);
                    Block block = world.getBlockState(mutable).getBlock();
                    String id = Registries.BLOCK.getId(block).toString();
                    if (targets.contains(id)) {
                        foundByBlock.computeIfAbsent(id, k -> new ArrayList<>()).add(mutable.toImmutable());
                    }
                }
            }
        }

        List<XRayEntry> finalEntries = new ArrayList<>();
        boolean showNames = TutorialMod.CONFIG.xrayShowNames;
        boolean clumping = TutorialMod.CONFIG.xrayClumpingEnabled;
        boolean textureMode = TutorialMod.CONFIG.xrayTextureMode;

        for (Map.Entry<String, List<BlockPos>> entry : foundByBlock.entrySet()) {
            String blockId = entry.getKey();
            String label = showNames ? blockId.substring(blockId.indexOf(':') + 1) : "";
            String textureName = "";
            if (textureMode) {
                extractTexture(blockId);
                textureName = blockId.contains(":") ? blockId.split(":")[1] : blockId;
            }

            if (clumping) {
                List<List<BlockPos>> clumps = findClumps(entry.getValue());
                for (List<BlockPos> clump : clumps) {
                    finalEntries.add(new XRayEntry(calculateCenter(clump), label, textureName));
                }
            } else {
                for (BlockPos pos : entry.getValue()) {
                    finalEntries.add(new XRayEntry(new Vec3d(pos.getX(), pos.getY(), pos.getZ()), label, textureName));
                }
            }
        }

        xrayEntries.clear();
        xrayEntries.addAll(finalEntries);
    }

    private List<List<BlockPos>> findClumps(List<BlockPos> positions) {
        List<List<BlockPos>> clumps = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> posSet = new HashSet<>(positions);

        for (BlockPos pos : positions) {
            if (visited.contains(pos)) continue;
            List<BlockPos> clump = new ArrayList<>();
            Queue<BlockPos> queue = new LinkedList<>();
            queue.add(pos);
            visited.add(pos);
            while (!queue.isEmpty()) {
                BlockPos curr = queue.poll();
                clump.add(curr);
                for (BlockPos neighbor : getNeighbors(curr)) {
                    if (posSet.contains(neighbor) && !visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
            clumps.add(clump);
        }
        return clumps;
    }

    private List<BlockPos> getNeighbors(BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        boolean twentySix = TutorialMod.CONFIG.xray26Adjacent;
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;
                    if (!twentySix) {
                        if (x * x + y * y + z * z > 2) continue;
                    }
                    neighbors.add(pos.add(x, y, z));
                }
            }
        }
        return neighbors;
    }

    private Vec3d calculateCenter(List<BlockPos> clump) {
        double x = 0, y = 0, z = 0;
        for (BlockPos pos : clump) {
            x += pos.getX();
            y += pos.getY();
            z += pos.getZ();
        }
        return new Vec3d(x / clump.size(), y / clump.size(), z / clump.size());
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
            float health = -1f;

            if (entity instanceof PlayerEntity player) {
                if (!TutorialMod.CONFIG.espPlayers || player.isInvisibleTo(client.player)) continue;
                if (TutorialMod.CONFIG.espShowNamesPlayers) label = player.getName().getString();
                if (TutorialMod.CONFIG.espShowDistance && dist >= TutorialMod.CONFIG.espDistanceHideThreshold) {
                    distLabel = String.format(Locale.ROOT, "%db", (int)dist);
                }
                color = TutorialMod.CONFIG.teamManager.isTeammate(player.getName().getString())
                        ? TutorialMod.CONFIG.espColorTeammate : TutorialMod.CONFIG.espColorEnemy;

                if (TutorialMod.CONFIG.espShowHealthBars) {
                    health = player.getHealth() / player.getMaxHealth();
                }
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

                Box box = entity.getBoundingBox().offset(x - entity.getX(), y - entity.getY(), z - entity.getZ()).offset(cameraPos.negate());
                projectAndAppend(boxesData, box, combinedMatrix, label, color, distLabel, true, health, "");
            }
        }

        // 2. Vanished Players
        if (TutorialMod.CONFIG.espAntiVanish) {
            for (Map.Entry<Integer, VanishedPlayerData> entry : vanishedPlayers.entrySet()) {
                Vec3d relPos = entry.getValue().pos.subtract(cameraPos);
                Box box = new Box(relPos.x - 0.3, relPos.y, relPos.z - 0.3, relPos.x + 0.3, relPos.y + 1.8, relPos.z + 0.3);
                projectAndAppend(boxesData, box, combinedMatrix, "Vanished", TutorialMod.CONFIG.espColorEnemy, "", true, -1f, "");
            }
        }

        // 3. X-Ray Blocks
        if (TutorialMod.CONFIG.xrayEnabled) {
            int color = TutorialMod.CONFIG.xrayColor;
            for (XRayEntry entry : xrayEntries) {
                Vec3d relPos = entry.pos.subtract(cameraPos);
                Box box = new Box(relPos.x, relPos.y, relPos.z, relPos.x + 1.0, relPos.y + 1.0, relPos.z + 1.0);
                projectAndAppend(boxesData, box, combinedMatrix, entry.label, color, "", false, -1f, entry.texture);
            }
        }

        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().updateBoxes(boxesData.toString());
    }

    private void projectAndAppend(StringBuilder data, Box box, Matrix4f combinedMatrix, String label, int color, String distLabel, boolean useWidthFactor, float health, String texture) {
        Vector4f[] corners = new Vector4f[]{
                new Vector4f((float)box.minX, (float)box.minY, (float)box.minZ, 1.0f),
                new Vector4f((float)box.maxX, (float)box.minY, (float)box.minZ, 1.0f),
                new Vector4f((float)box.minX, (float)box.maxY, (float)box.minZ, 1.0f),
                new Vector4f((float)box.maxX, (float)box.maxY, (float)box.minZ, 1.0f),
                new Vector4f((float)box.minX, (float)box.minY, (float)box.maxZ, 1.0f),
                new Vector4f((float)box.maxX, (float)box.minY, (float)box.maxZ, 1.0f),
                new Vector4f((float)box.minX, (float)box.maxY, (float)box.maxZ, 1.0f),
                new Vector4f((float)box.maxX, (float)box.maxY, (float)box.maxZ, 1.0f)
        };

        for (Vector4f corner : corners) {
            combinedMatrix.transform(corner);
        }

        List<Vector4f> points = new ArrayList<>();
        float epsilon = 0.01f;

        // Add corners that are in front
        for (Vector4f corner : corners) {
            if (corner.w > epsilon) points.add(corner);
        }

        // Add intersections for edges crossing the near plane
        int[][] edges = {{0,1}, {2,3}, {4,5}, {6,7}, {0,2}, {1,3}, {4,6}, {5,7}, {0,4}, {1,5}, {2,6}, {3,7}};
        for (int[] edge : edges) {
            Vector4f v1 = corners[edge[0]];
            Vector4f v2 = corners[edge[1]];

            if ((v1.w > epsilon) != (v2.w > epsilon)) {
                float t = (epsilon - v1.w) / (v2.w - v1.w);
                Vector4f intersect = new Vector4f(v1).lerp(v2, t);
                intersect.w = epsilon;
                points.add(intersect);
            }
        }

        if (points.isEmpty()) return;

        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;

        float fovScale = TutorialMod.CONFIG.espManualProjection ? (float)TutorialMod.CONFIG.espFovScale : 1.0f;
        float aspectScale = TutorialMod.CONFIG.espManualProjection ? (float)TutorialMod.CONFIG.espAspectRatioScale : 1.0f;

        for (Vector4f p : points) {
            float x = ((p.x / p.w) * fovScale * aspectScale + 1.0f) * 0.5f;
            float y = (1.0f - (p.y / p.w) * fovScale) * 0.5f;
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
        }

        float boxHeight = (maxY - minY) * (float)TutorialMod.CONFIG.espBoxScale;
        float boxWidth;
        float boxX;
        float boxY;

        if (useWidthFactor) {
            boxWidth = boxHeight * (float)TutorialMod.CONFIG.espBoxWidthFactor;
            boxX = (minX + maxX) / 2f - boxWidth / 2f;
            boxY = minY;
        } else if (!texture.isEmpty()) {
            // Unstretched square for textures, more stable
            float aspect = (float) client.getWindow().getWidth() / (float) client.getWindow().getHeight();
            float sizeH = Math.abs(maxY - minY) * (float)TutorialMod.CONFIG.xrayTextureScale;
            float sizeW = sizeH / aspect;
            boxWidth = sizeW;
            boxHeight = sizeH;
            boxX = (minX + maxX) / 2f - sizeW / 2f;
            boxY = (minY + maxY) / 2f - sizeH / 2f;
        } else {
            boxWidth = (maxX - minX) * (float)TutorialMod.CONFIG.espBoxScale;
            boxX = (minX + maxX) / 2f - boxWidth / 2f;
            boxY = (minY + maxY) / 2f - boxHeight / 2f;
        }

        if (data.length() > 0) data.append(";");
        data.append(String.format(Locale.ROOT, "%.4f,%.4f,%.4f,%.4f,%s,%d,%s,%.2f,%s", boxX, boxY, boxWidth, boxHeight, label, color, distLabel, health, texture));
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
        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand(String.format(Locale.ROOT, "HEALTH_BAR_WIDTH %.4f", (float)TutorialMod.CONFIG.espHealthBarWidth));
        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand("HEALTH_BAR_INVERTED " + TutorialMod.CONFIG.espHealthBarInverted);
        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand("HEALTH_BAR_SIDE " + TutorialMod.CONFIG.espHealthBarSide);
        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand(String.format(Locale.ROOT, "HEALTH_BAR_COLORS %d,%d,%d,%d",
            TutorialMod.CONFIG.espHealthBarColorFull, TutorialMod.CONFIG.espHealthBarColorMedium,
            TutorialMod.CONFIG.espHealthBarColorLow, TutorialMod.CONFIG.espHealthBarColorEmpty));
        net.rev.tutorialmod.TutorialModClient.getESPOverlayManager().sendCommand(String.format(Locale.ROOT, "TEXTURE_OPACITY %.4f", TutorialMod.CONFIG.xrayTextureOpacity / 100f));
    }
}
