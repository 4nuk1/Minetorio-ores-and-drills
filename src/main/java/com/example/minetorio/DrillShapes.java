package com.example.minetorio;

import net.minecraft.core.BlockPos;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Формы головок, считанные из OBJ-моделей (куб = 1 блок).
 * Слои идут снизу вверх (y = 0 - остриё), строки - ось Z, столбцы - ось X, '#' - блок drill_head.
 * Центр головки по X/Z - центральный блок остриЯ.
 */
public final class DrillShapes {
    private static final String[][] SMALL_LAYERS = {
        {"...", ".#.", "..."},
        {"...", ".#.", "..."},
        {".#.", "###", ".#."},
        {".#.", "###", ".#."},
        {"###", "###", "###"},
    };

    private static final String[][] LARGE_LAYERS = {
        {".....", ".....", "..#..", ".....", "....."},
        {".....", ".....", "..#..", ".....", "....."},
        {".....", "..#..", ".###.", "..#..", "....."},
        {".....", "..#..", ".###.", "..#..", "....."},
        {".....", ".###.", ".###.", ".###.", "....."},
        {"..#..", ".###.", "#####", ".###.", "..#.."},
        {"..#..", ".###.", "#####", ".###.", "..#.."},
        {".###.", "#####", "#####", "#####", ".###."},
    };

    public static final Set<BlockPos> SMALL = parse(SMALL_LAYERS);
    public static final Set<BlockPos> LARGE = parse(LARGE_LAYERS);

    private static Set<BlockPos> parse(String[][] layers) {
        Set<BlockPos> out = new HashSet<>();
        int half = layers[0].length / 2;
        for (int y = 0; y < layers.length; y++)
            for (int z = 0; z < layers[y].length; z++)
                for (int x = 0; x < layers[y][z].length(); x++)
                    if (layers[y][z].charAt(x) == '#') out.add(new BlockPos(x - half, y, z - half));
        return out;
    }

    /** Совпадают ли позиции блоков drill_head (в локальных координатах контрапции) с формой нужного бура. */
    public static boolean matches(Collection<BlockPos> heads, DrillTier tier) {
        Set<BlockPos> shape = tier == DrillTier.SMALL_3X3 ? SMALL : LARGE;
        if (heads.size() != shape.size()) return false;
        int minY = Integer.MAX_VALUE;
        for (BlockPos p : heads) minY = Math.min(minY, p.getY());
        BlockPos tip = null;
        for (BlockPos p : heads) {
            if (p.getY() == minY) {
                if (tip != null) return false; // в нижнем слое должен быть ровно один блок
                tip = p;
            }
        }
        for (BlockPos p : heads) {
            if (!shape.contains(p.subtract(tip))) return false;
        }
        return true;
    }

    private DrillShapes() {}
}
