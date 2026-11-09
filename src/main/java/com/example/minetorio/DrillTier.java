package com.example.minetorio;

/**
 * Стресс в Create = impact * RPM. Базовая скорость 32 RPM: 3x3 -> 1024 su, 5x5 -> 1536 su.
 * Производительность растёт только целыми ступенями (rpm / 32, до 8), поэтому всё кратно базовым значениям.
 */
public enum DrillTier {
    //          зона поиска, глубина, impact/RPM, блоков руды/цикл, ед. руды/цикл, блоков головки в колонне
    SMALL_3X3(2, 20, 32, 1, 1, 5),
    LARGE_5X5(3, 30, 48, 2, 3, 8);

    public final int platformHalf;
    public final int height;
    public final float impactPerRpm;
    public final int oreBlocksPerCycle;
    public final int rawPerCycle;
    private final int rods;

    DrillTier(int platformHalf, int height, float impactPerRpm, int oreBlocksPerCycle, int rawPerCycle, int rods) {
        this.platformHalf = platformHalf;
        this.height = height;
        this.impactPerRpm = impactPerRpm;
        this.oreBlocksPerCycle = oreBlocksPerCycle;
        this.rawPerCycle = rawPerCycle;
        this.rods = rods;
    }

    /** Сколько блоков "Блок головки бура" должно стоять колонной под основанием. */
    public int rods() { return rods; }
}
