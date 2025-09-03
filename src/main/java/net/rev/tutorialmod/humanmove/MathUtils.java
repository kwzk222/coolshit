package net.rev.tutorialmod.humanmove;

import java.util.List;
import java.util.Random;

public class MathUtils {
    public static final double TAU = Math.PI * 2;

    public static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    public static float wrapDeg(float a) {
        a %= 360f;
        if (a <= -180f) a += 360f;
        if (a > 180f) a -= 360f;
        return a;
    }

    public static float rFloat(float a, float b, Random r) {
        return a + r.nextFloat() * (b - a);
    }

    public static float[] concat(float[] a, float[] b) {
        float[] out = new float[a.length + b.length];
        System.arraycopy(a, 0, out, 0, a.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static float[] toArray(List<Float> L) {
        float[] a = new float[L.size()];
        for (int i = 0; i < L.size(); i++) a[i] = L.get(i);
        return a;
    }
}
