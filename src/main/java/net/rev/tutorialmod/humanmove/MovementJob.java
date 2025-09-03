package net.rev.tutorialmod.humanmove;

import java.util.Random;

final class MovementJob {
    final Mode mode;
    final float[] xs, ys;
    int i = 0;
    float currentX, currentY; // applied (after blending)

    MovementJob(Mode m, float[] xs, float[] ys) {
        this.mode = m;
        this.xs = xs;
        this.ys = ys;
    }

    boolean done() {
        return i >= xs.length;
    }

    enum Mode {
        CAMERA, GUI
    }
}

record Segment(float x0, float y0, float x1, float y1, float durMs) {
}

final class Precomputed {
    float[] xs, ys;
}

final class NoiseState {
    double tremorPhase1, tremorPhase2, driftPhase;

    NoiseState(Random r) {
        tremorPhase1 = r.nextDouble() * MathUtils.TAU;
        tremorPhase2 = r.nextDouble() * MathUtils.TAU;
        driftPhase = r.nextDouble() * MathUtils.TAU;
    }
}
