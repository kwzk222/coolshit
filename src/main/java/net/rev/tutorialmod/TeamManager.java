package net.rev.tutorialmod;

import java.util.ArrayList;
import java.util.List;

public class TeamManager {
    private List<String> friends;

    public TeamManager(List<String> friends) {
        this.friends = friends;
    }

    public void addFriend(String name) {
        if (!friends.contains(name)) {
            friends.add(name);
            TutorialMod.CONFIG.save();
        }
    }

    public void removeFriend(String name) {
        if (friends.contains(name)) {
            friends.remove(name);
            TutorialMod.CONFIG.save();
        }
    }

    public boolean isFriend(String name) {
        return friends.contains(name);
    }

    public List<String> getFriends() {
        return new ArrayList<>(friends);
    }
}
