package net.rev.tutorialmod;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.rev.tutorialmod.event.AttackEntityCallback;
import net.rev.tutorialmod.modules.*;

public class TutorialModClient implements ClientModInitializer {

    private static TutorialModClient instance;
    private TriggerBot triggerBot;
    private AutoTotem autoTotem;
    private static OverlayManager overlayManager;
    private AutoStun autoStun;
    private ChatMacros chatMacros;
    private Hotkeys hotkeys;
    private MinecartTech minecartTech;

    public static TutorialModClient getInstance() {
        return instance;
    }

    public AutoTotem getAutoTotem() {
        return autoTotem;
    }

    public static OverlayManager getOverlayManager() {
        return overlayManager;
    }

    public MinecartTech getMinecartTech() {
        return minecartTech;
    }

    @Override
    public void onInitializeClient() {
        instance = this;
        triggerBot = new TriggerBot();
        autoTotem = new AutoTotem();
        overlayManager = new OverlayManager();
        autoStun = new AutoStun();
        chatMacros = new ChatMacros();
        hotkeys = new Hotkeys();
        minecartTech = new MinecartTech();

        autoTotem.init();
        chatMacros.onInitializeClient();

        Runtime.getRuntime().addShutdownHook(new Thread(overlayManager::stop));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        AttackEntityCallback.EVENT.register(this::onAttackEntity);

        new CommandManager().registerCommands();
    }

    private void onClientTick(MinecraftClient client) {
        hotkeys.onClientTick(client);
        chatMacros.onClientTick(client);

        if (triggerBot != null) {
            triggerBot.onTick(client);
        }

        if (TutorialMod.CONFIG.showCoordsOverlay && overlayManager.isRunning() && client.player != null) {
            overlayManager.update(formatCoordsForOverlay(client));
        }

        TutorialMod.getAutoToolSwitch().onTick();

        if (!TutorialMod.CONFIG.masterEnabled) return;

        if (TutorialMod.CONFIG.autoTotemEnabled) {
            autoTotem.onTick(client);
        }

        autoStun.onClientTick(client);
        minecartTech.onClientTick(client);
    }

    private ActionResult onAttackEntity(PlayerEntity player, Entity target) {
        return autoStun.onAttackEntity(player, target);
    }

    public static void confirmRailPlacement(BlockPos pos, BlockState state) {
        if (MinecartTech.awaitingRailConfirmationCooldown > 0 && state.getBlock() instanceof net.minecraft.block.RailBlock) {
            if (instance != null) instance.getMinecartTech().startRailPlacement(pos);
            MinecartTech.awaitingRailConfirmationCooldown = -1;
        }
    }

    public static void confirmLavaPlacement(BlockPos pos, BlockState state) {
        MinecartTech.confirmLavaPlacement(pos, state, instance.getMinecartTech());
    }

    public static void confirmFirePlacement(BlockPos pos, BlockState state) {
        MinecartTech.confirmFirePlacement(pos, state, instance.getMinecartTech());
    }

    public static void recordBowUsage() {
        if (instance != null) {
            MinecartTech.lastBowShotTick = MinecraftClient.getInstance().world.getTime();
        }
    }

    private String formatCoordsForOverlay(MinecraftClient client) {
        if (client.player == null) return "";

        long bx = (long) Math.floor(client.player.getX());
        long by = (long) Math.floor(client.player.getY());
        long bz = (long) Math.floor(client.player.getZ());
        String coords = String.format("%d, %d, %d", bx, by, bz);

        String facing = "";
        try {
            Direction d = client.player.getHorizontalFacing();
            if (d != null) {
                facing = d.toString().substring(0, 1).toUpperCase() + d.toString().substring(1).toLowerCase();
            }
        } catch (Exception ignored) {}

        return coords + "|" + facing;
    }
}
