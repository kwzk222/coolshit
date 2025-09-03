package net.rev.tutorialmod.humanmove;

import net.rev.tutorialmod.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static net.rev.tutorialmod.humanmove.MathUtils.*;

public class MovementGenerator {

    static float minJerk(float s) {
        return (float) (10 * Math.pow(s, 3) - 15 * Math.pow(s, 4) + 6 * Math.pow(s, 5));
    }

    static float chooseDurationMs(float distance, ModConfig.MovementParams p, Random r) {
        float t = p.baseMs + p.kPerUnit * distance;
        t *= (0.9f + 0.2f * r.nextFloat()); // ±10% randomness
        return clamp(t, p.minMs, p.maxMs);
    }

    static List<Segment> planSegments(float x0, float y0, float x1, float y1, ModConfig.MovementParams p, Random r) {
        float dx = x1 - x0, dy = y1 - y0;
        float dist = (float) Math.hypot(dx, dy);
        float T = chooseDurationMs(dist, p, r);
        if (r.nextFloat() > p.viaPointChance || dist < 1f) {
            return List.of(new Segment(x0, y0, x1, y1, T));
        }
        // build via point
        float nx = -dy / dist, ny = dx / dist;                       // perpendicular
        float offFrac = lerp(p.viaOffsetMin, p.viaOffsetMax, r.nextFloat());
        float off = offFrac * dist * (r.nextBoolean() ? 1f : -1f);
        float t_via = 0.4f + 0.2f * r.nextFloat(); // where along the path via point is, 0.4-0.6
        float vx = x0 + dx * t_via + nx * off;
        float vy = y0 + dy * t_via + ny * off;

        float T1 = T * t_via;
        float T2 = T * (1-t_via);

        return List.of(
                new Segment(x0, y0, vx, vy, T1),
                new Segment(vx, vy, x1, y1, T2)
        );
    }

    static Precomputed precompute(List<Segment> segs, ModConfig.MovementParams p, Random r, float reactDelayMs) {
        int reactN = (int) Math.round(reactDelayMs * p.sampleHz / 1000.0);
        List<Float> X = new ArrayList<>(), Y = new ArrayList<>();
        // reaction hold
        for (int i = 0; i < reactN; i++) {
            X.add(segs.get(0).x0);
            Y.add(segs.get(0).y0);
        }
        // segments
        for (Segment s : segs) {
            int n = Math.max(2, (int) Math.round(s.durMs * p.sampleHz / 1000.0));
            for (int i = 0; i < n; i++) {
                float t = (n == 1) ? 1f : (float) i / (n - 1);
                float B = minJerk(t);
                X.add(lerp(s.x0, s.x1, B));
                Y.add(lerp(s.y0, s.y1, B));
            }
        }
        Precomputed out = new Precomputed();
        out.xs = toArray(X);
        out.ys = toArray(Y);
        return out;
    }

    static void applyHumanNoiseInPlace(Precomputed pc, ModConfig.MovementParams p, Random r, NoiseState ns) {
        int N = pc.xs.length;
        float alpha = p.alphaNoise * (1f + (r.nextFloat() * 2f - 1f) * p.noiseJitterFactor);
        for (int i = 1; i < N; i++) {
            float vx = (pc.xs[i] - pc.xs[i - 1]) * p.sampleHz;
            float vy = (pc.ys[i] - pc.ys[i - 1]) * p.sampleHz;
            float v = (float) Math.hypot(vx, vy);
            float sigma = alpha * v;

            // per-sample SD noise
            pc.xs[i] += (float) (r.nextGaussian() * sigma);
            pc.ys[i] += (float) (r.nextGaussian() * sigma);

            // tremor
            double t = i / (double) p.sampleHz;
            pc.xs[i] += p.tremorAmp * (float) Math.sin(TAU * p.tremorF1 * t + ns.tremorPhase1);
            pc.ys[i] += p.tremorAmp * (float) Math.cos(TAU * p.tremorF2 * t + ns.tremorPhase2);

            // drift (very small)
            pc.xs[i] += p.driftAmp * (float) Math.sin(TAU * p.driftHz * t + ns.driftPhase) * 0.02f;
            pc.ys[i] += p.driftAmp * (float) Math.cos(TAU * p.driftHz * t + ns.driftPhase) * 0.02f;
        }
    }

    static void maybeAppendCorrections(float targetX, float targetY,
                                       List<float[]> buffersXY, ModConfig.MovementParams p, Random r) {
        float[] xs = buffersXY.get(0);
        float[] ys = buffersXY.get(1);
        float curX = xs[xs.length - 1];
        float curY = ys[ys.length - 1];
        float err = (float) Math.hypot(targetX - curX, targetY - curY);
        int made = 0;
        while (err > p.correctionThreshold && made < p.maxCorrections) {
            float durMs = clamp((p.baseMs + p.kPerUnit * err) * p.correctionDurScale, 40f, 260f);
            Segment seg = new Segment(curX, curY, targetX, targetY, durMs);
            Precomputed pc = precompute(List.of(seg), p, r, rFloat(p.reactMinMs * 0.2f, p.reactMinMs * 0.6f, r));
            applyHumanNoiseInPlace(pc, p, r, new NoiseState(r));
            // append arrays
            buffersXY.set(0, concat(xs, pc.xs));
            buffersXY.set(1, concat(ys, pc.ys));
            xs = buffersXY.get(0);
            ys = buffersXY.get(1);
            curX = xs[xs.length - 1];
            curY = ys[ys.length - 1];
            err = (float) Math.hypot(targetX - curX, targetY - curY);
            made++;
        }
    }
}
