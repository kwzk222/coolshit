package net.rev.tutorialmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

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
                        TutorialMod.sendUpdateMessage("Your team list is currently empty.");
                    } else {
                        TutorialMod.sendUpdateMessage("Teammates: " + String.join(", ", teamManager.getTeammates()));
                    }
                    return 1;
                }));
        dispatcher.register(literal("tv")
                .executes(context -> {
                    TeamManager teamManager = TutorialMod.CONFIG.teamManager;
                    if (teamManager.getTeammates().isEmpty()) {
                        TutorialMod.sendUpdateMessage("Your team list is currently empty.");
                    } else {
                        TutorialMod.sendUpdateMessage("Teammates: " + String.join(", ", teamManager.getTeammates()));
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
                            if (TutorialMod.CONFIG.teamManager.addTeammate(name)) {
                                TutorialMod.sendUpdateMessage("Added " + name + " to your team.");
                            }
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
                            TutorialMod.sendUpdateMessage("Removed " + name + " from your team.");
                            return 1;
                        })));
        dispatcher.register(literal("tc")
                .executes(context -> {
                    TutorialMod.CONFIG.teamManager.clearTeammates();
                    TutorialMod.sendUpdateMessage("Team list has been cleared.");
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
                            TutorialMod.sendUpdateMessage("Sent message to " + teamManager.getTeammates().size() + " teammates.");
                            return 1;
                        })));
    }
}
