package com.example.minetorio;

/**
 * Параметры двух типов буров.
 *
 * Стресс в Create = impact * RPM. Базовая скорость 32 RPM:
 *   3x3: 32 * 32 = 1024 su;  5x5: 48 * 32 = 1536 su.
 * Производительность растёт ТОЛЬКО целыми ступенями (rpm / 32), поэтому
 * потребление и добыча всегда кратны базовым значениям: 32, 64, 96 ... 256 RPM.
 */
public enum DrillTier {
    //          полу-ширина платформы, высота, impact/RPM, блоков руды за цикл, ед. руды за цикл
    SMALL_3X3(2, 20, 32, 1, 1),
    LARGE_5X5(3, 30, 48, 2, 3);

    public final int platformHalf;   // 2 -> платформа 5x5, 3 -> 7x7 (зона поиска головки вокруг привода)
    public final int height;         // глубина хода головки вниз от привода (блоков)
    public final float impactPerRpm;
    public final int oreBlocksPerCycle;
    public final int rawPerCycle;

    DrillTier(int platformHalf, int height, float impactPerRpm, int oreBlocksPerCycle, int rawPerCycle) {
        this.platformHalf = platformHalf;
        this.height = height;
        this.impactPerRpm = impactPerRpm;
        this.oreBlocksPerCycle = oreBlocksPerCycle;
        this.rawPerCycle = rawPerCycle;
    }
}
