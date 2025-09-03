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

final class Segment {
    public final float x0, y0, x1, y1, durMs;

    Segment(float x0, float y0, float x1, float y1, float durMs) {
        this.x0 = x0;
        this.y0 = y0;
        this.x1 = x1;
        this.y1 = y1;
        this.durMs = durMs;
    }
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
