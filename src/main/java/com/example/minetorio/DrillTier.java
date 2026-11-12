package com.example.minetorio;

/**
 * Стресс в Create = impact * RPM. Базовая скорость 16 RPM: 3x3 -> 1024 su, 5x5 -> 1536 su.
 * Цикл добычи всегда длится 1 минуту. От скорости зависит только то, сколько руды даёт цикл.
 * Уровни скорости: 16 / 32 / 64 / 128 / 256 RPM (1..5).
 */
public enum DrillTier {
    //          impact/RPM, блоков руды по уровням,   ед. руды по уровням,    высота головки
    SMALL_3X3(64f, new int[]{1, 1, 2, 3, 4}, new int[]{1, 1, 2, 3, 4}, 5),
    LARGE_5X5(96f, new int[]{2, 2, 3, 3, 4}, new int[]{3, 4, 6, 8, 12}, 8);

    public static final int MAX_LEVEL = 5;

    public final float impactPerRpm;
    private final int[] oreBlocks;
    private final int[] raw;
    private final int shapeHeight;

    DrillTier(float impactPerRpm, int[] oreBlocks, int[] raw, int shapeHeight) {
        this.impactPerRpm = impactPerRpm;
        this.oreBlocks = oreBlocks;
        this.raw = raw;
        this.shapeHeight = shapeHeight;
    }

    /** Блоков руды за цикл на уровне скорости level (1..5). */
    public int oreBlocks(int level) { return oreBlocks[Math.max(1, Math.min(level, MAX_LEVEL)) - 1]; }

    /** Единиц руды (рейнджа) за цикл на уровне скорости level (1..5). */
    public int raw(int level) { return raw[Math.max(1, Math.min(level, MAX_LEVEL)) - 1]; }

    /** Высота головки: основание стоит на этой высоте над нижней точкой остриЯ. */
    public int shapeHeight() { return shapeHeight; }
}
