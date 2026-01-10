package net.rev.tutorialmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

public class TeamManager {
    private List<String> teammates;

    public TeamManager(List<String> teammates) {
        this.teammates = teammates;
    }

    public void addTeammate(String name) {
        if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getName().getString().equalsIgnoreCase(name)) {
            MinecraftClient.getInstance().player.sendMessage(Text.of("§cYou can't add yourself to your own teammates list."), false);
            return;
        }
        if (!teammates.contains(name)) {
            teammates.add(name);
            TutorialMod.CONFIG.save();
        }
    }

    public void removeTeammate(String name) {
        if (teammates.contains(name)) {
            teammates.remove(name);
            TutorialMod.CONFIG.save();
        }
    }

    public boolean isTeammate(String name) {
        return teammates.contains(name);
    }

    public List<String> getTeammates() {
        return new ArrayList<>(teammates);
    }

    public void clearTeammates() {
        teammates.clear();
        TutorialMod.CONFIG.save();
    }
}
