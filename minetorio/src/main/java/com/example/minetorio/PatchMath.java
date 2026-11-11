package com.example.minetorio;

/** Детерминированные хеш и шум: пласт одинаков при любом порядке генерации чанков. */
public final class PatchMath {
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double lattice(long seed, int x, int z) {
        long k = mix(seed ^ (x * 341873128712L) ^ (z * 132897987541L));
        return (k >>> 11) * (1.0 / (1L << 53));
    }

    /** Гладкий value-noise в диапазоне 0..1. */
    public static double noise(long seed, double x, double z) {
        int x0 = (int) Math.floor(x), z0 = (int) Math.floor(z);
        double fx = x - x0, fz = z - z0;
        double u = fx * fx * (3 - 2 * fx), v = fz * fz * (3 - 2 * fz);
        double a = lattice(seed, x0, z0), b = lattice(seed, x0 + 1, z0);
        double c = lattice(seed, x0, z0 + 1), d = lattice(seed, x0 + 1, z0 + 1);
        double top = a + (b - a) * u, bot = c + (d - c) * u;
        return top + (bot - top) * v;
    }

    private PatchMath() {}
}
