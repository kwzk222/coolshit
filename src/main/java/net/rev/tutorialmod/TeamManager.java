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

    public boolean addTeammate(String name) {
        if (MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().player.getName().getString().equalsIgnoreCase(name)) {
            TutorialMod.sendUpdateMessage("§cYou cannot add yourself to your own team.");
            return false;
        }
        if (!teammates.contains(name)) {
            teammates.add(name);
            TutorialMod.CONFIG.save();
            return true;
        }
        return false;
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
