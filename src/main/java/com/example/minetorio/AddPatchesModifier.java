package com.example.minetorio;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/**
 * Добавляет пласты руды в САМОЕ НАЧАЛО генерации биома: фаза BEFORE_EVERYTHING (раньше любых других
 * модификаторов) и самый первый шаг генерации RAW_GENERATION. Применяется ко всем биомам, кроме Края.
 */
public record AddPatchesModifier(HolderSet<PlacedFeature> features) implements BiomeModifier {
    public static final MapCodec<AddPatchesModifier> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(AddPatchesModifier::features)
    ).apply(i, AddPatchesModifier::new));

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.BEFORE_EVERYTHING) return;
        if (biome.is(BiomeTags.IS_END)) return;
        var gen = builder.getGenerationSettings();
        for (Holder<PlacedFeature> feature : features) {
            gen.addFeature(GenerationStep.Decoration.RAW_GENERATION, feature);
        }
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() { return CODEC; }
}
