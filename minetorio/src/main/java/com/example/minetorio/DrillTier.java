package com.example.minetorio;

/**
 * Стресс в Create = impact * RPM. Базовая скорость 32 RPM: 3x3 -> 1024 su, 5x5 -> 1536 su.
 * Ступень скорости = rpm / 32 (до 8). Каждая новая ступень умножает производительность на 1.5 (x1, x1.5, x2.25 ... x17.09).
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
