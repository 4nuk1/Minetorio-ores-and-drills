package com.example.minetorio;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.world.item.BlockItem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.ArrayList;
import java.util.function.Supplier;

public final class ModRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Minetorio.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Minetorio.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Minetorio.MOD_ID);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().strength(4.0f, 6.0f).sound(SoundType.NETHERITE_BLOCK)
                .noOcclusion().requiresCorrectToolForDrops();
    }

    public static final DeferredBlock<DrillControllerBlock> CONTROLLER_3X3 =
            BLOCKS.register("drill_controller_3x3", () -> new DrillControllerBlock(metal(), DrillTier.SMALL_3X3));
    public static final DeferredBlock<DrillControllerBlock> CONTROLLER_5X5 =
            BLOCKS.register("drill_controller_5x5", () -> new DrillControllerBlock(metal(), DrillTier.LARGE_5X5));

    /** Блок головки бура: из них игрок собирает форму 3x3 / 5x5. Сам ничего не ломает. */
    public static final DeferredBlock<Block> DRILL_HEAD =
            BLOCKS.register("drill_head", () -> new Block(metal().strength(5.0f, 8.0f)));
    /** Платформа под головку (того же размера, что и головка). */
    public static final DeferredBlock<Block> DRILL_PLATFORM =
            BLOCKS.register("drill_platform", () -> new Block(metal()));

    public static final DeferredItem<BlockItem> DRILL_HEAD_ITEM = ITEMS.registerSimpleBlockItem(DRILL_HEAD);
    public static final DeferredItem<BlockItem> DRILL_PLATFORM_ITEM = ITEMS.registerSimpleBlockItem(DRILL_PLATFORM);
    public static final DeferredItem<BlockItem> CONTROLLER_3X3_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER_3X3);
    public static final DeferredItem<BlockItem> CONTROLLER_5X5_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER_5X5);

    public static final Supplier<BlockEntityType<DrillControllerBlockEntity>> CONTROLLER_BE =
            BLOCK_ENTITIES.register("drill_controller", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new DrillControllerBlockEntity(ModRegistries.CONTROLLER_BE.get(), pos, state),
                    CONTROLLER_3X3.get(), CONTROLLER_5X5.get()).build(null));

    // --- генерация руд ---
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Minetorio.MOD_ID);
    public static final Supplier<OrePatchFeature> ORE_PATCH_FEATURE = FEATURES.register("ore_patches", OrePatchFeature::new);

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Minetorio.MOD_ID);
    public static final Supplier<MapCodec<? extends BiomeModifier>> REMOVE_ORES =
            BIOME_MODIFIERS.register("remove_ores", () -> RemoveOresModifier.CODEC);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Minetorio.MOD_ID);
    private static final MapCodec<LongOpenHashSet> INFINITE_CODEC =
            Codec.LONG.listOf().xmap(LongOpenHashSet::new, set -> new ArrayList<>(set)).fieldOf("positions");
    /** Позиции бесконечных руд в чанке (хранятся вместе с чанком). */
    public static final Supplier<AttachmentType<LongOpenHashSet>> INFINITE_ORES = ATTACHMENTS.register("infinite_ores",
            () -> AttachmentType.builder(() -> new LongOpenHashSet()).serialize(INFINITE_CODEC).build());

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
        BIOME_MODIFIERS.register(bus);
        ATTACHMENTS.register(bus);
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
    }

    private ModRegistries() {}
}
