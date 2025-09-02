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
    }
}
