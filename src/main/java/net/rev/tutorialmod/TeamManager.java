package net.rev.tutorialmod;

import java.util.ArrayList;
import java.util.List;

public class TeamManager {
    private List<String> teammates;

    public TeamManager(List<String> teammates) {
        this.teammates = teammates;
    }

    public void addTeammate(String name) {
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
