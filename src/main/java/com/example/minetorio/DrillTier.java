package com.example.minetorio;

/**
 * Стресс в Create = impact * RPM. Базовая скорость 32 RPM: 3x3 -> 1024 su, 5x5 -> 1536 su.
 * Производительность растёт только целыми ступенями (rpm / 32, до 8), поэтому всё кратно базовым значениям.
 */
public enum DrillTier {
    //          impact/RPM, блоков руды/цикл, ед. руды/цикл, высота головки (блоков)
    SMALL_3X3(32, 1, 1, 5),
    LARGE_5X5(48, 2, 3, 8);

    public final float impactPerRpm;
    public final int oreBlocksPerCycle;
    public final int rawPerCycle;
    private final int shapeHeight;

    DrillTier(float impactPerRpm, int oreBlocksPerCycle, int rawPerCycle, int shapeHeight) {
        this.impactPerRpm = impactPerRpm;
        this.oreBlocksPerCycle = oreBlocksPerCycle;
        this.rawPerCycle = rawPerCycle;
        this.shapeHeight = shapeHeight;
    }

    /** Высота головки: основание стоит на этой высоте над нижней точкой стержня. */
    public int shapeHeight() { return shapeHeight; }
}
