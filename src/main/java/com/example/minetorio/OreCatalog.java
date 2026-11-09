package com.example.minetorio;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;

import java.util.*;

/**
 * Список всех руд (в том числе модовых): берётся из тега c:ores в момент генерации.
 * spacing - размер ячейки в блоках (по одному пласту на ячейку, расстояние между соседними пластами < 2*spacing),
 * radius - базовый радиус пласта в блоках.
 */
public final class OreCatalog {
    public record OreType(Block block, int spacing, int radius, long salt) {}

    /** id -> {spacing, radius}. Всё, чего здесь нет (модовые руды), получает DEFAULT. */
    private static final int[] DEFAULT = {700, 9};
    private static final Map<String, int[]> PARAMS = Map.ofEntries(
            Map.entry("minecraft:coal_ore", new int[]{440, 10}),
            Map.entry("minecraft:iron_ore", new int[]{480, 10}),
            Map.entry("minecraft:copper_ore", new int[]{520, 10}),
            Map.entry("minecraft:gold_ore", new int[]{960, 8}),
            Map.entry("minecraft:redstone_ore", new int[]{1000, 8}),
            Map.entry("minecraft:lapis_ore", new int[]{1100, 7}),
            Map.entry("minecraft:emerald_ore", new int[]{1400, 6}),
            Map.entry("minecraft:diamond_ore", new int[]{1800, 6}),
            Map.entry("minecraft:nether_gold_ore", new int[]{520, 9}),
            Map.entry("minecraft:nether_quartz_ore", new int[]{480, 10}),
            Map.entry("minecraft:ancient_debris", new int[]{1400, 5})
    );

    private static volatile List<OreType> overworld;
    private static volatile List<OreType> nether;

    public static void clear() { overworld = null; nether = null; }

    public static List<OreType> get(boolean inNether) {
        if (overworld == null) build();
        return inNether ? nether : overworld;
    }

    private static synchronized void build() {
        if (overworld != null) return;
        List<Block> blocks = new ArrayList<>();
        for (Holder<Block> h : BuiltInRegistries.BLOCK.getTagOrEmpty(Tags.Blocks.ORES)) blocks.add(h.value());
        blocks.sort(Comparator.comparing(b -> BuiltInRegistries.BLOCK.getKey(b).toString()));

        List<OreType> ow = new ArrayList<>(), ne = new ArrayList<>();
        for (Block b : blocks) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            String path = id.getPath();
            BlockState s = b.defaultBlockState();
            if (path.contains("deepslate") || s.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)) continue; // вариант не нужен
            boolean isNether = s.is(Tags.Blocks.ORES_IN_GROUND_NETHERRACK)
                    || path.contains("nether") || path.equals("ancient_debris");
            int[] p = PARAMS.getOrDefault(id.toString(), DEFAULT);
            OreType t = new OreType(b, p[0], p[1], id.toString().hashCode() * 0x9E3779B97F4A7C15L);
            (isNether ? ne : ow).add(t);
        }
        nether = ne;
        overworld = ow; // последним: по нему проверяется готовность
    }

    private OreCatalog() {}
}
