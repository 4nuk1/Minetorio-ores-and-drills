package com.example.minetorio;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Set;

/**
 * Формы головок, считанные из OBJ-моделей (куб = 1 блок). Слои идут снизу вверх (y = 0 - остриё),
 * строки - ось Z, столбцы - ось X, центр головки по X/Z - центральная колонна (x = 0, z = 0).
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

    private static final Set<BlockPos> SMALL = parse(SMALL_LAYERS);   // 21 блок
    private static final Set<BlockPos> LARGE = parse(LARGE_LAYERS);   // 68 блоков

    private static Set<BlockPos> parse(String[][] layers) {
        Set<BlockPos> out = new HashSet<>();
        int half = layers[0].length / 2;
        for (int y = 0; y < layers.length; y++)
            for (int z = 0; z < layers[y].length; z++)
                for (int x = 0; x < layers[y][z].length(); x++)
                    if (layers[y][z].charAt(x) == '#') out.add(new BlockPos(x - half, y, z - half));
        return out;
    }

    /** Клетки головки относительно центра самого нижнего блока (остриё). */
    public static Set<BlockPos> cells(DrillTier tier) {
        return tier == DrillTier.SMALL_3X3 ? SMALL : LARGE;
    }

    private DrillShapes() {}
}
