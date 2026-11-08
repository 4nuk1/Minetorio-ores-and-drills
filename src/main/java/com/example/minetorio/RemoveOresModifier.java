package com.example.minetorio;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/** Убирает обычную генерацию руд (ванильных и модовых) из биомов Верхнего и Нижнего мира. */
public record RemoveOresModifier() implements BiomeModifier {
    public static final MapCodec<RemoveOresModifier> CODEC = MapCodec.unit(new RemoveOresModifier());

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.REMOVE) return;
        if (!biome.is(BiomeTags.IS_OVERWORLD) && !biome.is(BiomeTags.IS_NETHER)) return;
        var gen = builder.getGenerationSettings();
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) {
            gen.getFeatures(step).removeIf(RemoveOresModifier::isOreFeature);
        }
    }

    private static boolean isOreFeature(Holder<PlacedFeature> holder) {
        try {
            var config = holder.value().feature().value().config();
            if (!(config instanceof OreConfiguration oc)) return false;
            for (OreConfiguration.TargetBlockState t : oc.targetStates) {
                Block b = t.state.getBlock();
                String path = BuiltInRegistries.BLOCK.getKey(b).getPath();
                if (path.contains("infested")) continue;
                // теги в этот момент могут быть ещё не привязаны, поэтому дублируем проверку по имени
                boolean byName = path.endsWith("_ore") || path.contains("_ore_") || path.startsWith("ore_")
                        || path.equals("ancient_debris");
                boolean byTag = false;
                try { byTag = t.state.is(Tags.Blocks.ORES); } catch (Exception ignored) {}
                if (byName || byTag) return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() { return CODEC; }
}
