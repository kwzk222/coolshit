package net.rev.tutorialmod;

import net.rev.tutorialmod.humanmove.HumanMoveController;

public class Human {
    public static HumanMoveController move() {
        return HumanMoveController.getInstance();
    }
}
