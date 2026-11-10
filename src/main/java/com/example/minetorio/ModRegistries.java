package com.example.minetorio;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModRegistries {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Minetorio.MOD_ID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Minetorio.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Minetorio.MOD_ID);
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, Minetorio.MOD_ID);

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().strength(4.0f, 6.0f).sound(SoundType.NETHERITE_BLOCK)
                .noOcclusion().requiresCorrectToolForDrops();
    }

    /** Основание головки: принимает вращение сверху и выдвигает вниз стержень. */
    public static final DeferredBlock<DrillHeadBaseBlock> DRILL_HEAD_BASE =
            BLOCKS.register("drill_head_base", () -> new DrillHeadBaseBlock(metal()));
    /** Блок головки (незерит): из них игрок строит головку по форме 3x3 / 5x5. */
    public static final DeferredBlock<Block> DRILL_HEAD =
            BLOCKS.register("drill_head", () -> new Block(BlockBehaviour.Properties.of().strength(5.0f, 8.0f)
                    .sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops()));
    /** Скрытый блок: только носитель модели стержня (его рисует сущность). В мире не ставится, предмета нет. */
    public static final DeferredBlock<Block> DRILL_ROD_BLOCK =
            BLOCKS.register("drill_rod", () -> new Block(BlockBehaviour.Properties.of().noCollission().noLootTable().noOcclusion()));
    public static final DeferredBlock<DrillControllerBlock> CONTROLLER_3X3 =
            BLOCKS.register("drill_drive_3x3", () -> new DrillControllerBlock(metal(), DrillTier.SMALL_3X3));
    public static final DeferredBlock<DrillControllerBlock> CONTROLLER_5X5 =
            BLOCKS.register("drill_drive_5x5", () -> new DrillControllerBlock(metal(), DrillTier.LARGE_5X5));

    public static final DeferredItem<BlockItem> DRILL_HEAD_BASE_ITEM = ITEMS.registerSimpleBlockItem(DRILL_HEAD_BASE);
    public static final DeferredItem<BlockItem> DRILL_HEAD_ITEM = ITEMS.registerSimpleBlockItem(DRILL_HEAD);
    public static final DeferredItem<BlockItem> CONTROLLER_3X3_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER_3X3);
    public static final DeferredItem<BlockItem> CONTROLLER_5X5_ITEM = ITEMS.registerSimpleBlockItem(CONTROLLER_5X5);

    public static final Supplier<BlockEntityType<DrillControllerBlockEntity>> CONTROLLER_BE =
            BLOCK_ENTITIES.register("drill_drive", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new DrillControllerBlockEntity(ModRegistries.CONTROLLER_BE.get(), pos, state),
                    CONTROLLER_3X3.get(), CONTROLLER_5X5.get()).build(null));

    public static final Supplier<BlockEntityType<DrillHeadBaseBlockEntity>> BASE_BE =
            BLOCK_ENTITIES.register("drill_head_base", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new DrillHeadBaseBlockEntity(ModRegistries.BASE_BE.get(), pos, state),
                    DRILL_HEAD_BASE.get()).build(null));

    public static final Supplier<EntityType<DrillBitEntity>> DRILL_BIT =
            ENTITY_TYPES.register("drill_bit", () -> EntityType.Builder.<DrillBitEntity>of(DrillBitEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(10).updateInterval(1).noSave().noSummon().fireImmune()
                    .build("minetorio:drill_bit"));

    public static final Supplier<EntityType<DrillRodEntity>> DRILL_ROD =
            ENTITY_TYPES.register("drill_rod", () -> EntityType.Builder.<DrillRodEntity>of(DrillRodEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(10).updateInterval(1).noSave().noSummon().fireImmune()
                    .build("minetorio:drill_rod"));

    // --- генерация руд ---
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, Minetorio.MOD_ID);
    public static final Supplier<OrePatchFeature> ORE_PATCH_FEATURE = FEATURES.register("ore_patches", OrePatchFeature::new);

    public static final DeferredRegister<MapCodec<? extends BiomeModifier>> BIOME_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Minetorio.MOD_ID);
    public static final Supplier<MapCodec<? extends BiomeModifier>> REMOVE_ORES =
            BIOME_MODIFIERS.register("remove_ores", () -> RemoveOresModifier.CODEC);

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, Minetorio.MOD_ID);

    /** Позиция -> прочность. Хранится списком [ключ, значение, ключ, значение ...]. */
    private static final Codec<Long2IntOpenHashMap> ORE_HEALTH_CODEC = Codec.LONG.listOf().xmap(
            list -> {
                Long2IntOpenHashMap map = new Long2IntOpenHashMap();
                for (int i = 0; i + 1 < list.size(); i += 2) map.put(list.get(i).longValue(), list.get(i + 1).intValue());
                return map;
            },
            map -> {
                List<Long> out = new ArrayList<>(map.size() * 2);
                map.long2IntEntrySet().forEach(e -> { out.add(e.getLongKey()); out.add((long) e.getIntValue()); });
                return out;
            });
    public static final Supplier<AttachmentType<Long2IntOpenHashMap>> ORE_HEALTH = ATTACHMENTS.register("ore_health",
            () -> AttachmentType.builder(() -> new Long2IntOpenHashMap()).serialize(ORE_HEALTH_CODEC).build());

    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        ENTITY_TYPES.register(bus);
        FEATURES.register(bus);
        BIOME_MODIFIERS.register(bus);
        ATTACHMENTS.register(bus);
    }

    private ModRegistries() {}
}
