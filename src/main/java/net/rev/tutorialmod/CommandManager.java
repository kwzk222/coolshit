package net.rev.tutorialmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.CommandRegistryAccess;
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
                    if (teamManager.getFriends().isEmpty()) {
                        context.getSource().sendFeedback(Text.of("You have no friends on your list."));
                    } else {
                        context.getSource().sendFeedback(Text.of("Friends: " + String.join(", ", teamManager.getFriends())));
                    }
                    return 1;
                }));

        dispatcher.register(literal("ta")
                .then(argument("name", StringArgumentType.word())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            TutorialMod.CONFIG.teamManager.addFriend(name);
                            context.getSource().sendFeedback(Text.of("Added " + name + " to your friends list."));
                            return 1;
                        })));

        dispatcher.register(literal("tr")
                .then(argument("name", StringArgumentType.word())
                        .executes(context -> {
                            String name = StringArgumentType.getString(context, "name");
                            TutorialMod.CONFIG.teamManager.removeFriend(name);
                            context.getSource().sendFeedback(Text.of("Removed " + name + " from your friends list."));
                            return 1;
                        })));
    }
}
