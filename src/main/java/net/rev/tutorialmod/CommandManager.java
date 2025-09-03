package net.rev.tutorialmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class CommandManager {
    public void registerCommands() {
        ClientCommandRegistrationCallback.EVENT.register(this::register);
    }

    private void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(literal("ts")
                .executes(context -> {
                    TeamManager teamManager = TutorialMod.CONFIG.teamManager;
                    if (teamManager.getTeammates().isEmpty()) {
                        context.getSource().sendFeedback(Text.of("You have no teammates on your list."));
                    } else {
                        context.getSource().sendFeedback(Text.of("Teammates: " + String.join(", ", teamManager.getTeammates())));
                    }
                    return 1;
                }));

        dispatcher.register(literal("ta")
                .then(argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                context.getSource().getClient().getNetworkHandler().getPlayerList().stream().map(p -> p.getProfile().getName()),
                                builder))
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            TutorialMod.CONFIG.teamManager.addTeammate(name);
                            context.getSource().sendFeedback(Text.of("Added " + name + " to your teammates list."));
                            return 1;
                        })));

        dispatcher.register(literal("tr")
                .then(argument("name", StringArgumentType.word())
                        .suggests((context, builder) -> CommandSource.suggestMatching(
                                TutorialMod.CONFIG.teamManager.getTeammates(),
                                builder))
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            TutorialMod.CONFIG.teamManager.removeTeammate(name);
                            context.getSource().sendFeedback(Text.of("Removed " + name + " from your teammates list."));
                            return 1;
                        })));
        dispatcher.register(literal("tc")
                .executes(context -> {
                    TutorialMod.CONFIG.teamManager.clearTeammates();
                    context.getSource().sendFeedback(Text.of("Teammate list cleared."));
                    return 1;
                }));
        dispatcher.register(literal("tm")
                .then(argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            String message = StringArgumentType.getString(context, "message");
                            TeamManager teamManager = TutorialMod.CONFIG.teamManager;
                            for (String teammate : teamManager.getTeammates()) {
                                context.getSource().getClient().player.networkHandler.sendChatCommand("msg " + teammate + " " + message);
                            }
                            context.getSource().sendFeedback(Text.of("Message sent to " + teamManager.getTeammates().size() + " teammates."));
                            return 1;
                        })));

        dispatcher.register(literal("testmove")
                .then(literal("camera")
                        .executes(context -> {
                            MinecraftClient client = context.getSource().getClient();
                            if (client.player != null) {
                                Vec3d target = client.player.getPos().add(5, 2, 5);
                                Human.move().lookAt(target);
                                context.getSource().sendFeedback(Text.of("Testing camera movement..."));
                            }
                            return 1;
                        }))
                .then(literal("gui")
                        .executes(context -> {
                            MinecraftClient client = context.getSource().getClient();
                            if (client.currentScreen != null) {
                                float x = client.getWindow().getScaledWidth() / 2f;
                                float y = client.getWindow().getScaledHeight() / 2f;
                                Human.move().startGuiMove(x, y);
                                context.getSource().sendFeedback(Text.of("Testing GUI movement..."));
                            } else {
                                context.getSource().sendError(Text.of("You must be in a GUI to test GUI movement."));
                            }
                            return 1;
                        })));
    }
}
