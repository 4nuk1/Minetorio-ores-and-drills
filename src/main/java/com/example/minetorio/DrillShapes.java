package com.example.minetorio;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Форма вращающейся головки (из OBJ-моделей). Слои идут снизу вверх (y = 0 - остриё),
 * строки - ось Z, столбцы - ось X. Центральная колонна (x = 0, z = 0) занята блоками головки,
 * поэтому сущность рисует только остальные кубы.
 */
public final class DrillShapes {
    private static final String[][] SMALL_LAYERS = {
        {"...", ".#.", "..."}, {"...", ".#.", "..."},
        {".#.", "###", ".#."}, {".#.", "###", ".#."},
        {"###", "###", "###"},
    };
    private static final String[][] LARGE_LAYERS = {
        {".....", ".....", "..#..", ".....", "....."}, {".....", ".....", "..#..", ".....", "....."},
        {".....", "..#..", ".###.", "..#..", "....."}, {".....", "..#..", ".###.", "..#..", "....."},
        {".....", ".###.", ".###.", ".###.", "....."},
        {"..#..", ".###.", "#####", ".###.", "..#.."}, {"..#..", ".###.", "#####", ".###.", "..#.."},
        {".###.", "#####", "#####", "#####", ".###."},
    };

    private static final Set<BlockPos> VISUAL_SMALL = parse(SMALL_LAYERS);
    private static final Set<BlockPos> VISUAL_LARGE = parse(LARGE_LAYERS);

    private static Set<BlockPos> parse(String[][] layers) {
        Set<BlockPos> out = new HashSet<>();
        int half = layers[0].length / 2;
        for (int y = 0; y < layers.length; y++)
            for (int z = 0; z < layers[y].length; z++)
                for (int x = 0; x < layers[y][z].length(); x++)
                    if (layers[y][z].charAt(x) == '#' && !(x == half && z == half)) // без центральной колонны
                        out.add(new BlockPos(x - half, y, z - half));
        return out;
    }

    /** Клетки визуальной головки относительно центра самого нижнего блока колонны. */
    public static Set<BlockPos> visualCells(DrillTier tier) {
        return tier == DrillTier.SMALL_3X3 ? VISUAL_SMALL : VISUAL_LARGE;
    }

    private DrillShapes() {}
}
