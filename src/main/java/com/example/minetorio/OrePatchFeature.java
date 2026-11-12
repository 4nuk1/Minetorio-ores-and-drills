package com.example.minetorio;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Огромные поверхностные пласты руды в стиле Factorio.
 * Для каждого вида руды мир поделён на ячейки; в каждой ячейке ровно один пласт со случайным центром.
 * Пласт лежит целиком внутри своей ячейки, поэтому чанку достаточно проверить одну ячейку на руду.
 * Каждому блоку пласта в данных чанка записывается прочность (см. OreHealth).
 */
public class OrePatchFeature extends Feature<NoneFeatureConfiguration> {
    public OrePatchFeature() { super(NoneFeatureConfiguration.CODEC); }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        if (level.getLevel().dimension() == Level.END) return false;
        // измерение с потолком (Нижний мир и подобные) получает пласты на полу пещер, остальные - на поверхности
        boolean inNether = level.dimensionType().hasCeiling();

        int baseX = ctx.origin().getX() & ~15;
        int baseZ = ctx.origin().getZ() & ~15;
        int chunkX = baseX >> 4, chunkZ = baseZ >> 4;
        long seed = level.getSeed();
        ChunkAccess chunk = level.getChunk(chunkX, chunkZ);
        boolean placedAny = false;
        Long2IntOpenHashMap marks = null;

        for (OreCatalog.OreType ore : OreCatalog.get(inNether)) {
            int cellChunks = Math.max(8, Math.round(ore.spacing() / 16f));
            int span = cellChunks * 16;
            int cellX = Math.floorDiv(chunkX, cellChunks), cellZ = Math.floorDiv(chunkZ, cellChunks);
            int radius = ore.radius();
            int reach = (int) (radius * 1.4) + 1;      // шум раздувает пласт максимум до 1.4 радиуса
            int margin = reach + 2;
            int free = span - 2 * margin;

            long h = PatchMath.mix(seed ^ ore.salt() ^ (cellX * 0x632BE59BD9B4E019L) ^ (cellZ * 0x9E3779B97F4A7C15L));
            int cx = cellX * span + margin + (int) Math.floorMod(h, (long) free);
            int cz = cellZ * span + margin + (int) Math.floorMod(PatchMath.mix(h), (long) free);
            if (cx + reach < baseX || cx - reach > baseX + 15 || cz + reach < baseZ || cz - reach > baseZ + 15) continue;
            long nseed = PatchMath.mix(h ^ 0x5DEECE66DL);

            for (int lx = 0; lx < 16; lx++) {
                for (int lz = 0; lz < 16; lz++) {
                    int x = baseX + lx, z = baseZ + lz;
                    double dx = x - cx, dz = z - cz;
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist > reach) continue;
                    // форма пласта: радиальный спад + крупный и мелкий шум (рваный край как в Factorio)
                    double f = 1.0 - dist / radius
                            + 0.50 * (PatchMath.noise(nseed, x / 9.0, z / 9.0) - 0.5)
                            + 0.25 * (PatchMath.noise(nseed + 1, x / 4.0, z / 4.0) - 0.5);
                    if (f <= 0) continue;

                    int top = inNether ? netherFloor(level, x, z)
                                       : level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                    if (top < level.getMinBuildHeight() + 1) continue;
                    // прочность убывает от центра пласта к краю: ~2000 в центре, ~100 на краю
                    int hp = OreHealth.MIN_HEALTH + (int) Math.round((OreHealth.MAX_HEALTH - OreHealth.MIN_HEALTH) * Math.min(1.0, f));
                    int depth = Math.min(6, 2 + (int) (f * 4));   // толщина пласта по рельефу: 2..6 блоков

                    for (int y = top; y > top - depth; y--) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState old = level.getBlockState(pos);
                        if (old.isAir() || !old.getFluidState().isEmpty() || old.getDestroySpeed(level, pos) < 0) break;
                        level.setBlock(pos, ore.block().defaultBlockState(), 2);
                        if (marks == null) marks = chunk.getData(ModRegistries.ORE_HEALTH.get());
                        marks.put(pos.asLong(), hp);
                        placedAny = true;
                    }
                }
            }
        }
        if (placedAny) chunk.setUnsaved(true);
        return placedAny;
    }

    /** Измерение с потолком: пол первой пещеры под потолочной массой, не лавовое море. -1 если подходящего нет. */
    private static int netherFloor(WorldGenLevel level, int x, int z) {
        int min = level.getMinBuildHeight();
        int top = min + level.dimensionType().logicalHeight() - 18;   // 110 для Нижнего мира
        int bottom = min + 33;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, top, z);
        int y = top;
        while (y > bottom && level.getBlockState(p.setY(y)).blocksMotion()) y--;   // выйти из потолка
        while (y > bottom && !level.getBlockState(p.setY(y)).blocksMotion()) y--;  // найти пол
        if (y <= bottom) return -1;
        if (!level.getBlockState(p.setY(y + 1)).getFluidState().isEmpty()) return -1;
        return y;
    }
}
